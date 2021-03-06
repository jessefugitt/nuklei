package org.kaazing.nuklei.amqp_1_0.aeron;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import org.kaazing.nuklei.amqp_1_0.api.CanonicalMessage;
import org.kaazing.nuklei.amqp_1_0.api.ConnectionlessTransportAdapter;
import uk.co.real_logic.aeron.Aeron;
import uk.co.real_logic.aeron.Publication;
import uk.co.real_logic.aeron.Subscription;
import uk.co.real_logic.aeron.common.concurrent.logbuffer.DataHandler;
import uk.co.real_logic.aeron.common.concurrent.logbuffer.Header;
import uk.co.real_logic.aeron.driver.MediaDriver;
import uk.co.real_logic.agrona.CloseHelper;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.IoUtil;
import uk.co.real_logic.agrona.concurrent.BackoffIdleStrategy;
import uk.co.real_logic.agrona.concurrent.IdleStrategy;

/**
 *
 */
public class AeronStaticTransportAdapter implements ConnectionlessTransportAdapter
{
    private static final boolean PROXY_CREATION_AT_STARTUP = AeronAdapterConfiguration.PROXY_CREATION_AT_STARTUP;
    private static final int FRAGMENT_COUNT_LIMIT = AeronAdapterConfiguration.FRAGMENT_COUNT_LIMIT;
    private static final boolean DISABLE_EMBEDDED_MEDIA_DRIVER = AeronAdapterConfiguration.DISABLE_EMBEDDED_MEDIA_DRIVER;

    private final ThreadLocal<AeronMessage> tlAeronMessage = new ThreadLocal<AeronMessage>()
    {
        @Override
        protected AeronMessage initialValue()
        {
            return new AeronMessage();
        }
    };

    protected MediaDriver driver;
    private String uniqueAeronDir;
    protected AeronWrapper aeronWrapper;
    protected final AeronLogicalNameMapping logicalNameMapping = new AeronLogicalNameMapping();
    protected final Map<AeronPhysicalStream, Publication> proxyPublicationMap = new HashMap<>();
    protected final Map<AeronPhysicalStream, Subscription> proxySubscriptionsMap = new HashMap<>();
    final ExecutorService executor = Executors.newFixedThreadPool(1);

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final IdleStrategy idleStrategy = new BackoffIdleStrategy(
            100, 10, TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.MICROSECONDS.toNanos(100));
    private final CopyOnWriteArrayList<BiConsumer<String, CanonicalMessage>> messageListeners = new CopyOnWriteArrayList<>();

    protected Properties loadPropertiesFromFile(String fileName) throws IOException
    {
        Properties props = new Properties();
        InputStream input = getClass().getClassLoader().getResourceAsStream(fileName);
        if(input == null)
        {
            throw new FileNotFoundException("Properties file not found: " + fileName);
        }
        else
        {
            props.load(input);
        }
        if(input != null)
        {
            input.close();
        }
        return props;
    }

    //TODO(JAF): This is just a quick implementation using properties files to load the static config
    // It could be replaced by a more robust config method or a dynamic discovery option
    protected void loadStaticProxyPublicationsAndSubscriptions() throws IOException
    {
        Properties publicationsProperties = loadPropertiesFromFile("publications.properties");
        Properties subscriptionsProperties = loadPropertiesFromFile("subscriptions.properties");
        Enumeration e = publicationsProperties.propertyNames();

        while (e.hasMoreElements())
        {
            String logicalName = (String) e.nextElement();
            String publications = publicationsProperties.getProperty(logicalName);
            String[] channelStreams = publications.split(";");
            for(String channelStream : channelStreams)
            {
                AeronPhysicalStream physicalStream = AeronPhysicalStream.fromString(channelStream);
                logicalNameMapping.loadPublication(logicalName, physicalStream);
                if (PROXY_CREATION_AT_STARTUP)
                {
                    addProxyPublication(logicalName, physicalStream);
                }
            }
        }

        e = subscriptionsProperties.propertyNames();

        while (e.hasMoreElements())
        {
            String logicalName = (String) e.nextElement();
            String subscriptions = subscriptionsProperties.getProperty(logicalName);
            String[] channelStreams = subscriptions.split(";");
            for(String channelStream : channelStreams)
            {
                AeronPhysicalStream physicalStream = AeronPhysicalStream.fromString(channelStream);
                logicalNameMapping.loadSubscription(logicalName, physicalStream);
                if(PROXY_CREATION_AT_STARTUP)
                {
                    addProxySubscription(logicalName, physicalStream);
                }
            }

        }
    }

    @Override
    public void start()
    {
        //starts a unique embedded driver by default unless disabled
        if(DISABLE_EMBEDDED_MEDIA_DRIVER)
        {
            System.out.println("Disabling embedded media driver");
            driver = null;
        }
        else
        {
            uniqueAeronDir = IoUtil.tmpDirName() + "aeron" + File.separator + UUID.randomUUID().toString();
            System.out.println("Starting embedded media driver at dir: " + uniqueAeronDir);
            MediaDriver.Context driverContext = new MediaDriver.Context();
            driverContext.dirName(uniqueAeronDir);
            driverContext.dirsDeleteOnExit(true);
            driver = MediaDriver.launch(driverContext);
        }

        final Aeron.Context ctx = new Aeron.Context();
        if(uniqueAeronDir != null)
        {
            ctx.dirName(uniqueAeronDir);
        }
        Aeron aeron = Aeron.connect(ctx);
        aeronWrapper = new AeronWrapper(aeron);
        try
        {
            loadStaticProxyPublicationsAndSubscriptions();
        }
        catch (IOException e)
        {
            //TODO(JAF): Add error handling and logging
            e.printStackTrace();
        }
        executor.execute(() -> pollSubscribers());
    }

    @Override
    public void stop()
    {
        executor.shutdown();
        running.set(false);
        try
        {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        for(Publication publication : proxyPublicationMap.values())
        {
            publication.close();
        }
        for(Subscription subscription : proxySubscriptionsMap.values())
        {
            subscription.close();
        }

        aeronWrapper.close();

        CloseHelper.quietClose(driver);

        //if(Boolean.getBoolean(CommonContext.DIRS_DELETE_ON_EXIT_PROP_NAME))
        //{
            //TODO(JAF): If Aeron doesn't clean up the unique "aeron.dir" parent folder then we may need to do it here
        //}
    }


    protected void addProxyPublication(String logicalName, AeronPhysicalStream physicalStream)
    {
        Publication publication = aeronWrapper.addPublication(physicalStream.getChannel(), physicalStream.getStreamId());
        proxyPublicationMap.put(physicalStream, publication);
    }

    protected void addProxySubscription(String logicalName, AeronPhysicalStream physicalStream)
    {
        DataHandler dataHandler = new DataHandler()
        {
            @Override
            public void onData(DirectBuffer buffer, int offset, int length, Header header)
            {
                //TODO(JAF): Don't create a new object here but maybe there is a better way than thread local
                AeronMessage message = tlAeronMessage.get();
                message.setBuffer(buffer);
                message.setOffset(offset);
                message.setLength(length);
                message.setHeader(header);

                System.out.println("Received message on aeron (" + physicalStream.getChannel() + " " +
                        physicalStream.getStreamId() + ") which is configured to send to logical name (" + logicalName + ")");
                onLocalMessageReceived(logicalName, message);
            }
        };
        System.out.println("Subscribing to: " + physicalStream.getChannel() + " " + physicalStream.getStreamId());
        Subscription subscription = aeronWrapper.addSubscription(
                physicalStream.getChannel(), physicalStream.getStreamId(), dataHandler);
        proxySubscriptionsMap.put(physicalStream, subscription);
    }

    public void pollSubscribers()
    {
        try
        {
            while (running.get())
            {
                int fragmentsRead = 0;
                //TODO(JAF): Keep the subscribers in a garbage free list so that we can iterate without garbage
                for(Subscription subscription : proxySubscriptionsMap.values())
                {
                    fragmentsRead += subscription.poll(FRAGMENT_COUNT_LIMIT);
                }
                idleStrategy.idle(fragmentsRead);
            }
        }
        catch (final Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public Publication deleteProxyPublication(AeronPhysicalStream physicalStream)
    {
        Publication publication = proxyPublicationMap.remove(physicalStream);
        if(publication != null)
        {
            publication.close();
        }
        return publication;
    }

    public Subscription deleteProxySubscription(AeronPhysicalStream physicalStream)
    {
        Subscription subscription = proxySubscriptionsMap.remove(physicalStream);
        if(subscription != null)
        {
            subscription.close();
        }
        return subscription;
    }

    @Override
    public void onRemoteProducerDetected(String logicalName, String uniqueId)
    {
        if(PROXY_CREATION_AT_STARTUP)
        {
            //Don't create the proxy publications based on events
        }
        else
        {
            //Create the proxy publications based on events
            List<AeronPhysicalStream> physicalStreams = logicalNameMapping.getPublications(logicalName);
            if(physicalStreams != null && physicalStreams.size() > 0)
            {
                for(AeronPhysicalStream physicalStream : physicalStreams)
                {
                    Publication publication = proxyPublicationMap.get(physicalStream);
                    if(publication != null)
                    {
                        //Log that we already have this proxy publication mapped for this logical name
                    }
                    else
                    {
                        addProxyPublication(logicalName, physicalStream);
                    }
                }
            }
            else
            {
                //TODO(JAF): This is where normally you would create a proxy publication and let subscribers
                // subscribe but this isn't currently supported with Aeron and the static mapping
            }
        }
    }

    @Override
    public void onRemoteProducerRemoved(String logicalName, String uniqueId)
    {
        if(PROXY_CREATION_AT_STARTUP)
        {
            //Don't need to remove the proxy publications based on remote events
        }
        else
        {
            //TODO(JAF): Typically you'd do some bookkeeping to track the number of remote producers
            // and then only remove the proxy publication when the count goes to 0 (often lazily or based on a timeout)
            // or else you would have a different proxy publication per uniqueId
            List<AeronPhysicalStream> physicalStreams = logicalNameMapping.getPublications(logicalName);
            if(physicalStreams != null && physicalStreams.size() > 0)
            {
                for(AeronPhysicalStream physicalStream : physicalStreams)
                {
                    deleteProxyPublication(physicalStream);
                }
            }
        }
    }

    @Override
    public void onRemoteConsumerDetected(String logicalName, String uniqueId)
    {
        if(PROXY_CREATION_AT_STARTUP)
        {
            //Don't create the proxy publications based on events
        }
        else
        {
            //Create the proxy publications based on events
            List<AeronPhysicalStream> physicalStreams = logicalNameMapping.getSubscriptions(logicalName);
            if(physicalStreams != null && physicalStreams.size() > 0)
            {
                for(AeronPhysicalStream physicalStream : physicalStreams)
                {
                    Subscription subscription = proxySubscriptionsMap.get(physicalStream);
                    if(subscription != null)
                    {
                        //Log that we already have this proxy publication mapped for this logical name
                    }
                    else
                    {
                        addProxySubscription(logicalName, physicalStream);
                    }
                }
            }
            else
            {
                //TODO(JAF): This is where normally you would create a proxy subscription and let subscribers
                // subscribe but this isn't currently supported with Aeron and the static mapping
            }
        }
    }

    @Override
    public void onRemoteConsumerRemoved(String logicalName, String uniqueId)
    {
        if(PROXY_CREATION_AT_STARTUP)
        {
            //Don't need to remove the proxy subscription based on remote events
        }
        else
        {
            //TODO(JAF): Typically you'd do some bookkeeping to track the number of remote consumers
            // and then only remove the proxy subscription when the count goes to 0 (often lazily or based on a timeout)
            // or else you would have a different proxy subscription per uniqueId
            List<AeronPhysicalStream> physicalStreams = logicalNameMapping.getSubscriptions(logicalName);
            if(physicalStreams != null && physicalStreams.size() > 0)
            {
                for(AeronPhysicalStream physicalStream : physicalStreams)
                {
                    deleteProxySubscription(physicalStream);
                }
            }
        }
    }

    @Override
    public void onRemoteMessageReceived(String logicalName, CanonicalMessage canonicalMessage)
    {
        //TODO(JAF): Convert these messages and pass them out the local side
        List<AeronPhysicalStream> physicalStreams = logicalNameMapping.getLogicalNameToPublicationsMap().get(logicalName);
        if(physicalStreams != null && physicalStreams.size() > 0)
        {
            for(AeronPhysicalStream physicalStream : physicalStreams)
            {
                Publication publication = proxyPublicationMap.get(physicalStream);
                if(publication != null)
                {
                    long result = publication.offer(canonicalMessage.getBuffer(), canonicalMessage.getOffset(),
                            canonicalMessage.getLength());
                    //if(result == false)
                    //{
                    //TODO(JAF): Add error handling on failure
                    //}
                }
            }
        }
        else
        {
            //TODO(JAF): Log that we are dropping the message
        }
    }

    @Override
    public void onLocalProducerDetected(String logicalName, String uniqueId)
    {
        //In a static transport, there won't be events for the detection of local producers
    }

    @Override
    public void onLocalProducerRemoved(String logicalName, String uniqueId)
    {
        //In a static transport, there won't be events for the removal of local producers
    }

    @Override
    public void onLocalConsumerDetected(String logicalName, String uniqueId)
    {
        //In a static transport, there won't be events for the detection of local consumers
    }

    @Override
    public void onLocalConsumerRemoved(String logicalName, String uniqueId)
    {
        //In a static transport there won't be events for the removal of local consumers
    }

    @Override
    public void addLocalMessageReceivedListener(BiConsumer<String, CanonicalMessage> messageHandler)
    {
        messageListeners.add(messageHandler);
    }

    @Override
    public void removeLocalMessageReceivedListener(BiConsumer<String, CanonicalMessage> messageHandler)
    {
        messageListeners.remove(messageHandler);
    }

    @Override
    public void onLocalMessageReceived(String logicalName, CanonicalMessage message)
    {
        for(BiConsumer<String, CanonicalMessage> messageListener : messageListeners)
        {
            messageListener.accept(logicalName, message);
        }
    }

    //Use primarily for unit testing since Aeron is final and can't be mocked
    class AeronWrapper
    {
        private final Aeron aeron;
        public AeronWrapper(Aeron aeron)
        {
            this.aeron = aeron;
        }
        public Subscription addSubscription(String channel, int streamId, DataHandler dataHandler)
        {
            return aeron.addSubscription(channel, streamId, dataHandler);
        }
        public Publication addPublication(String channel, int streamId)
        {
            return aeron.addPublication(channel, streamId);
        }
        public void close()
        {
            aeron.close();
        }
    }
}
