package org.kaazing.nuklei.amqp_1_0.aeron;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.function.BiConsumer;

import org.junit.Before;
import org.junit.Test;

import uk.co.real_logic.aeron.Publication;
import uk.co.real_logic.aeron.Subscription;
import uk.co.real_logic.aeron.common.concurrent.logbuffer.DataHandler;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public class AeronTransportAdapterTest
{
    private AeronStaticTransportAdapter aeronStaticTransportAdapter;
    private int localMessageHandlerCount;
    @Before
    public void setUp()
    {
        localMessageHandlerCount = 0;
        BiConsumer<String, CanonicalMessage> testLocalMessageHandler = new BiConsumer<String, CanonicalMessage>()
        {
            @Override
            public void accept(String logicalName, CanonicalMessage message)
            {
                localMessageHandlerCount++;
            }
        };
        aeronStaticTransportAdapter = new AeronStaticTransportAdapter(testLocalMessageHandler);
        aeronStaticTransportAdapter.aeronWrapper = mock(AeronStaticTransportAdapter.AeronWrapper.class);
    }

    @Test
    public void testLoadPropertiesFromFile() throws IOException
    {
        //topic.ABC = udp://localhost:30123,10|udp://localhost:40123,10
        //topic.DEF = udp://localhost:30123,11
        String fileName = "test_publications.properties";
        Properties testProps = aeronStaticTransportAdapter.loadPropertiesFromFile(fileName);
        assertEquals("udp://localhost:30123,10|udp://localhost:40123,10", testProps.getProperty("topic.ABC"));
        assertEquals("udp://localhost:30123,11", testProps.getProperty("topic.DEF"));
    }

    @Test
    public void testAddDeleteProxyPublication()
    {
        AeronPhysicalStream physicalStream = new AeronPhysicalStream("udp://localhost:30123,10", 99);
        Publication mockPublication = mock(Publication.class);
        when(mockPublication.channel()).thenReturn(physicalStream.getChannel());
        when(mockPublication.streamId()).thenReturn(physicalStream.getStreamId());
        when(aeronStaticTransportAdapter.aeronWrapper.addPublication(physicalStream.getChannel(),
                        physicalStream.getStreamId())).thenReturn(mockPublication);

        aeronStaticTransportAdapter.addProxyPublication("topic.test.1", physicalStream);
        verify(aeronStaticTransportAdapter.aeronWrapper).addPublication(physicalStream.getChannel(),
                physicalStream.getStreamId());

        Publication proxyPublication = aeronStaticTransportAdapter.deleteProxyPublication(physicalStream);
        assertNotNull(proxyPublication);
        assertEquals(physicalStream.getChannel(), proxyPublication.channel());
        assertEquals(physicalStream.getStreamId(), proxyPublication.streamId());
        assertNull(aeronStaticTransportAdapter.deleteProxyPublication(physicalStream));
    }

    @Test
    public void testAddDeleteProxySubscription()
    {
        AeronPhysicalStream physicalStream = new AeronPhysicalStream("udp://localhost:30123,10", 99);
        Subscription mockSubscription = mock(Subscription.class);
        when(mockSubscription.channel()).thenReturn(physicalStream.getChannel());
        when(mockSubscription.streamId()).thenReturn(physicalStream.getStreamId());
        when(aeronStaticTransportAdapter.aeronWrapper.addSubscription(eq(physicalStream.getChannel()),
                        eq(physicalStream.getStreamId()), any(DataHandler.class))).thenReturn(mockSubscription);

        aeronStaticTransportAdapter.addProxySubscription("topic.test.1", physicalStream);
        verify(aeronStaticTransportAdapter.aeronWrapper).addSubscription(eq(physicalStream.getChannel()),
                eq(physicalStream.getStreamId()), any(DataHandler.class));

        Subscription proxySubscription = aeronStaticTransportAdapter.deleteProxySubscription(physicalStream);
        assertNotNull(proxySubscription);
        assertEquals(physicalStream.getChannel(), proxySubscription.channel());
        assertEquals(physicalStream.getStreamId(), proxySubscription.streamId());
        assertNull(aeronStaticTransportAdapter.deleteProxySubscription(physicalStream));
    }

    @Test
    public void testOnRemoteMessageReceived()
    {
        String logicalName = "topic.test.1";
        AeronPhysicalStream physicalStream = new AeronPhysicalStream("udp://localhost:30123,10", 99);
        aeronStaticTransportAdapter.logicalNameMapping.loadPublication(logicalName, physicalStream);

        Publication mockPublication = mock(Publication.class);
        when(mockPublication.channel()).thenReturn(physicalStream.getChannel());
        when(mockPublication.streamId()).thenReturn(physicalStream.getStreamId());
        when(aeronStaticTransportAdapter.aeronWrapper.addPublication(physicalStream.getChannel(),
                        physicalStream.getStreamId())).thenReturn(mockPublication);

        aeronStaticTransportAdapter.onRemoteProducerDetected(logicalName);
        verify(aeronStaticTransportAdapter.aeronWrapper).addPublication(physicalStream.getChannel(),
                physicalStream.getStreamId());

        CanonicalMessage mockCanonicalMessage = mock(CanonicalMessage.class);
        aeronStaticTransportAdapter.onRemoteMessageReceived(logicalName, mockCanonicalMessage);

        verify(mockPublication).offer(mockCanonicalMessage.getBuffer(), mockCanonicalMessage.getOffset(),
                mockCanonicalMessage.getLength());
    }

    @Test
    public void testOnLocalMessageReceived()
    {
        String logicalName = "topic.test.1";
        AeronPhysicalStream physicalStream = new AeronPhysicalStream("udp://localhost:30123,10", 99);
        aeronStaticTransportAdapter.logicalNameMapping.loadSubscription(logicalName, physicalStream);

        Subscription mockSubscription = mock(Subscription.class);
        when(mockSubscription.channel()).thenReturn(physicalStream.getChannel());
        when(mockSubscription.streamId()).thenReturn(physicalStream.getStreamId());
        when(aeronStaticTransportAdapter.aeronWrapper.addSubscription(eq(physicalStream.getChannel()),
                eq(physicalStream.getStreamId()), any(DataHandler.class))).thenReturn(mockSubscription);

        aeronStaticTransportAdapter.onRemoteConsumerDetected(logicalName);
        verify(aeronStaticTransportAdapter.aeronWrapper).addSubscription(eq(physicalStream.getChannel()),
                eq(physicalStream.getStreamId()), any(DataHandler.class));

        int previousMessageHandlerCount = localMessageHandlerCount;
        assertEquals(0, previousMessageHandlerCount);
        AeronMessage mockMessage = mock(AeronMessage.class);
        aeronStaticTransportAdapter.onLocalMessageReceived(logicalName, physicalStream, mockMessage);
        assertEquals(previousMessageHandlerCount + 1, localMessageHandlerCount);
    }

    public static void main(String[] args) throws InterruptedException
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(256));
        AeronStaticTransportAdapter adapter = new AeronStaticTransportAdapter(
                (logicalName, message) -> System.out.println("Received message that maps back to logical name: " + logicalName));

        String payload = "Hello World!";
        byte[] payloadBytes = payload.getBytes();
        buffer.putBytes(0, payloadBytes);
        final int length = payloadBytes.length;
        adapter.start();

        //Simulate a remote producer
        adapter.onRemoteProducerDetected("topic.ABC");
        adapter.onRemoteMessageReceived("topic.ABC", new CanonicalMessage()
        {
            @Override
            public DirectBuffer getBuffer()
            {
                return buffer;
            }

            @Override
            public int getOffset()
            {
                return 0;
            }

            @Override
            public int getLength()
            {
                return length;
            }

        });

        try
        {
            //Wait for 10 seconds before closing
            Thread.sleep(10000);
        }
        catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        adapter.stop();
    }
}
