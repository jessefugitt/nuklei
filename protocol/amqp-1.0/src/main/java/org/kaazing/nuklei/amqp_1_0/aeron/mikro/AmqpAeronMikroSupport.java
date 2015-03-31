package org.kaazing.nuklei.amqp_1_0.aeron.mikro;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.kaazing.nuklei.amqp_1_0.codec.definitions.Role.RECEIVER;
import static org.kaazing.nuklei.amqp_1_0.codec.messaging.Performative.*;
import static org.kaazing.nuklei.amqp_1_0.codec.transport.Header.AMQP_PROTOCOL;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import org.kaazing.nuklei.amqp_1_0.AmqpMikroFactory;
import org.kaazing.nuklei.amqp_1_0.aeron.AeronStaticTransportAdapter;
import org.kaazing.nuklei.amqp_1_0.aeron.AeronTransportAdapter;
import org.kaazing.nuklei.amqp_1_0.aeron.CanonicalMessage;
import org.kaazing.nuklei.amqp_1_0.codec.definitions.ReceiverSettleMode;
import org.kaazing.nuklei.amqp_1_0.codec.definitions.Role;
import org.kaazing.nuklei.amqp_1_0.codec.definitions.SenderSettleMode;
import org.kaazing.nuklei.amqp_1_0.codec.messaging.*;
import org.kaazing.nuklei.amqp_1_0.codec.transport.*;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Header;
import org.kaazing.nuklei.amqp_1_0.connection.Connection;
import org.kaazing.nuklei.amqp_1_0.connection.ConnectionFactory;
import org.kaazing.nuklei.amqp_1_0.connection.ConnectionHandler;
import org.kaazing.nuklei.amqp_1_0.connection.ConnectionHooks;
import org.kaazing.nuklei.amqp_1_0.connection.ConnectionStateMachine;
import org.kaazing.nuklei.amqp_1_0.link.Link;
import org.kaazing.nuklei.amqp_1_0.link.LinkFactory;
import org.kaazing.nuklei.amqp_1_0.link.LinkHandler;
import org.kaazing.nuklei.amqp_1_0.link.LinkHooks;
import org.kaazing.nuklei.amqp_1_0.link.LinkStateMachine;
import org.kaazing.nuklei.amqp_1_0.sender.Sender;
import org.kaazing.nuklei.amqp_1_0.sender.SenderFactory;
import org.kaazing.nuklei.amqp_1_0.sender.TcpSenderFactory;
import org.kaazing.nuklei.amqp_1_0.session.Session;
import org.kaazing.nuklei.amqp_1_0.session.SessionFactory;
import org.kaazing.nuklei.amqp_1_0.session.SessionHandler;
import org.kaazing.nuklei.amqp_1_0.session.SessionHooks;
import org.kaazing.nuklei.amqp_1_0.session.SessionStateMachine;
import org.kaazing.nuklei.function.DirectBufferAccessor;
import org.kaazing.nuklei.function.Mikro;
import org.kaazing.nuklei.function.MutableDirectBufferMutator;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public class AmqpAeronMikroSupport
{
    private static final int SEND_BUFFER_SIZE = 1024;
    /*
    public static final ThreadLocal<UnsafeBuffer> SEND_BUFFER = new ThreadLocal<UnsafeBuffer>()
    {
        @Override
        protected UnsafeBuffer initialValue()
        {
            return new UnsafeBuffer(ByteBuffer.allocateDirect(SEND_BUFFER_SIZE));
        }
    };
    */

    private final BiConsumer<String, CanonicalMessage> canonicalMessageHandler = new BiConsumer<String, CanonicalMessage>()
    {
        @Override
        public void accept(String logicalName, CanonicalMessage canonicalMessage)
        {
            //TODO(JAF): This should be a list
            AmqpConsumer consumer = amqpConsumerMap.get(logicalName);
            if(consumer != null)
            {
                final byte[] data = new byte[canonicalMessage.getLength()];
                canonicalMessage.getBuffer().getBytes(canonicalMessage.getOffset(), data);

                Link<AmqpLink> link = consumer.link;
                Sender sender = link.sender;
                long deliveryId = consumer.deliveryCounter.getAndIncrement();

                Frame frame = Frame.LOCAL_REF.get();
                Transfer transfer = Transfer.LOCAL_REF.get();

                frame.wrap(sender.getBuffer(), sender.getOffset(), true)
                        .setDataOffset(2)
                        .setType(0)
                        .setChannel(0)
                        .setPerformative(TRANSFER);
                transfer.wrap(sender.getBuffer(), frame.bodyOffset(), true)
                        .setHandle(consumer.handle)
                        .setDeliveryId(deliveryId)
                        .setDeliveryTag(WRITE_UTF_8, "\0")
                        .setMessageFormat(0)
                        .setSettled(true);
                transfer.getMessage()
                        .setPayloadOnly(true)
                        .setDescriptor(0x77L)
                        .setValue(WRITE_UTF_8, new String(data, UTF_8)
                        );

                frame.setLength(transfer.limit() - frame.offset());

                //TODO(JAF): Figure out why different...
                System.out.println(frame.limit() + " " + transfer.limit());

                link.send(frame, transfer);

            }
            //TODO(JAF): This is where aeron canonical messages will be passed from local publications
            /*
            // find the target session, get sendBuffer for said session, new Transfer frame to that session with
            // just decoded Message body
            sendFrame.wrap(sender.getBuffer(), sender.getOffset(), true)
                    .setDataOffset(2)
                    .setType(0)
                    .setChannel(0)
                    .setPerformative(TRANSFER);
            sendTransfer.wrap(sender.getBuffer(), sendFrame.bodyOffset(), true)
                    .setHandle(0)
                    .setDeliveryId(0)
                    .setDeliveryTag(WRITE_UTF_8, "\0")
                    .setMessageFormat(0)
                    .setSettled(true);
            sendTransfer.getMessage()
                    .setDescriptor(0x77L)
                    .setValue(WRITE_UTF_8, messageString);
            sendFrame.setLength(sendTransfer.limit() - sendFrame.offset());
            */
            //TODO(JAF): Lookup receiver link and send it
            //receiverLink.send(sendFrame, sendTransfer);
        }
    };

    private final AeronTransportAdapter aeronTransportAdapter = new AeronStaticTransportAdapter(canonicalMessageHandler);

    //TODO(JAF): Need to convert to multimap to support multiple producers/consumers with same logical address
    private final Map<String, AmqpProducer> amqpProducerMap = new ConcurrentHashMap<String, AmqpProducer>();
    private final Map<String, AmqpConsumer> amqpConsumerMap = new ConcurrentHashMap<String, AmqpConsumer>();

    public AmqpAeronMikroSupport()
    {
        //TODO(JAF): This should probably only be started once
        aeronTransportAdapter.start();
    }

    public void close()
    {
        aeronTransportAdapter.stop();
    }

    public Mikro createAmqpAeronMikro()
    {
        final SenderFactory senderFactory = new TcpSenderFactory(new UnsafeBuffer(ByteBuffer.allocate(SEND_BUFFER_SIZE)));
        final ConnectionFactory<AmqpConnection, AmqpSession, AmqpLink> connectionFactory =
                new AmqpConnectionFactory();
        final ConnectionHandler<AmqpConnection, AmqpSession, AmqpLink> connectionHandler =
                new ConnectionHandler<AmqpConnection, AmqpSession, AmqpLink>(
                        new AmqpSessionFactory(),
                        new SessionHandler<AmqpSession, AmqpLink>(
                                new AmqpLinkFactory(amqpProducerMap, amqpConsumerMap, aeronTransportAdapter),
                                new LinkHandler<AmqpLink>()));

        AmqpMikroFactory<AmqpConnection, AmqpSession, AmqpLink> factory =
                new AmqpMikroFactory<AmqpConnection, AmqpSession, AmqpLink>();
        return factory.newMikro(senderFactory, connectionFactory, connectionHandler);
    }

    private class AmqpConnection extends Connection<AmqpConnection, AmqpSession, AmqpLink>
    {
        // TODO: the JmsConnection stored a javax.jms.Connection object representing the connection to the
        //       broker, is something similar needed here for the connection to the Aeron bus?
        public AmqpConnection(ConnectionFactory<AmqpConnection, AmqpSession, AmqpLink> connectionFactory,
                              ConnectionStateMachine<AmqpConnection, AmqpSession, AmqpLink> stateMachine,
                              Sender sender, MutableDirectBuffer reassemblyBuffer)
        {
            super(stateMachine, sender, reassemblyBuffer);
            this.parameter = this;
        }
    }

    private class AmqpSession extends Session<AmqpSession, AmqpLink>
    {
        AmqpSession(Connection<AmqpConnection, AmqpSession, AmqpLink> owner,
                    SessionStateMachine<AmqpSession, AmqpLink> stateMachine)
        {
            super(stateMachine, owner.sender);
            this.parameter = this;
        }
    }

    private class AmqpLink extends Link<AmqpLink>
    {
        protected Map<String, AmqpProducer> producerMap;
        protected Map<String, AmqpConsumer> consumerMap;
        protected AeronTransportAdapter aeronTransportAdapter;
        protected String linkAddress;
        public AmqpLink(Session<AmqpSession, AmqpLink> owner,
                        LinkStateMachine<AmqpLink> stateMachine,
                        Map<String, AmqpProducer> producerMap,
                        Map<String, AmqpConsumer> consumerMap, AeronTransportAdapter aeronTransportAdapter)
        {
            super(stateMachine, owner.sender);
            this.parameter = this;
            this.producerMap = producerMap;
            this.consumerMap = consumerMap;
            this.aeronTransportAdapter = aeronTransportAdapter;
        }
    }

    private class AmqpConnectionFactory implements ConnectionFactory<AmqpConnection, AmqpSession, AmqpLink>
    {
        @Override
        public Connection<AmqpConnection, AmqpSession, AmqpLink> newConnection(Sender sender,
                                                                                           MutableDirectBuffer reassemblyBuffer)
        {
            ConnectionStateMachine<AmqpConnection, AmqpSession, AmqpLink> stateMachine =
                    new ConnectionStateMachine<AmqpConnection, AmqpSession, AmqpLink>(
                            new AmqpConnectionHooks());
            return new AmqpConnection(this, stateMachine, sender, reassemblyBuffer);
        }
    }

    private class AmqpSessionFactory implements SessionFactory<AmqpConnection, AmqpSession, AmqpLink>
    {
        @Override
        public Session<AmqpSession, AmqpLink> newSession(
                Connection<AmqpConnection, AmqpSession, AmqpLink> connection)
        {
            return new AmqpSession(connection,
                    new SessionStateMachine<AmqpSession, AmqpLink>(
                            new AmqpSessionHooks()));
        }
    }

    private class AmqpLinkFactory implements LinkFactory<AmqpSession, AmqpLink>
    {
        private final Map<String, AmqpProducer> localProducerMap;
        private final Map<String, AmqpConsumer> localConsumerMap;
        private final AeronTransportAdapter aeronTransportAdapter;

        public AmqpLinkFactory(Map<String, AmqpProducer> localProducerMap, Map<String, AmqpConsumer> localConsumerMap,
                               AeronTransportAdapter aeronTransportAdapter)
        {
            this.localProducerMap = localProducerMap;
            this.localConsumerMap = localConsumerMap;
            this.aeronTransportAdapter = aeronTransportAdapter;
        }

        @Override
        public Link<AmqpLink> newLink(Session<AmqpSession, AmqpLink> session)
        {
            return new AmqpLink(session, new LinkStateMachine<AmqpLink>(
                    new AmqpLinkHooks()), localProducerMap, localConsumerMap, aeronTransportAdapter);
                    //new LinkHooks<AmqpTestLink>()));
        }
    }

    private static class AmqpLinkHooks extends LinkHooks<AmqpLink>
    {
        //private static final Frame sendFrame = new Frame();
        //private static final Transfer sendTransfer = new Transfer();
        private static final AmqpMessage MESSAGE = new AmqpMessage();

        public AmqpLinkHooks()
        {
            whenAttachReceived = AmqpLinkHooks::whenAttachReceived;
            whenTransferReceived = AmqpLinkHooks::whenTransferReceived;
            whenDetachReceived = AmqpLinkHooks::whenDetachReceived;
        }

        private static void whenAttachReceived(Link<AmqpLink> link, Frame frame, Attach attach)
        {

            Role gatewayRole = null;
            AmqpLink parameter = link.parameter;
            String attachName = attach.getName(READ_UTF_8);
            String sourceAddress = attach.getSource().getAddress(READ_UTF_8);
            String targetAddress = attach.getTarget().getAddress(READ_UTF_8);
            long handle = attach.getHandle();

            if (attach.getRole() == Role.RECEIVER)
            {
                parameter.linkAddress = sourceAddress;
                AmqpConsumer localConsumer = new AmqpConsumer(parameter.linkAddress, link, handle);
                parameter.consumerMap.put(parameter.linkAddress, localConsumer);
                parameter.aeronTransportAdapter.onRemoteConsumerDetected(parameter.linkAddress);
                gatewayRole = Role.SENDER;
            }
            else
            {
                parameter.linkAddress = targetAddress;
                AmqpProducer localProducer = new AmqpProducer(parameter.linkAddress, link, handle);
                parameter.producerMap.put(parameter.linkAddress, localProducer);
                parameter.aeronTransportAdapter.onRemoteProducerDetected(parameter.linkAddress);

                gatewayRole = Role.RECEIVER;
            }

            Sender sender = link.sender;
            frame.wrap(sender.getBuffer(), sender.getOffset(), true)
                  .setDataOffset(2)
                  .setType(0)
                  .setChannel(0)
                  .setPerformative(ATTACH);
            attach.wrap(sender.getBuffer(), frame.bodyOffset(), true)
                  .maxLength(255)
                  .setName(WRITE_UTF_8, attachName)
                  .setHandle(handle)
                  .setRole(gatewayRole)
                  .setSendSettleMode(SenderSettleMode.MIXED)
                  .setReceiveSettleMode(ReceiverSettleMode.FIRST);

            if(gatewayRole == Role.RECEIVER)
            {
                attach.getSource()
                        .setDescriptor()
                        .maxLength(255)
                        .setAddress(WRITE_UTF_8, sourceAddress)
                        .setDurable(TerminusDurability.NONE)
                        .setExpiryPolicy(TerminusExpiryPolicy.SESSION_END) //.setExpiryPolicy(TerminusExpiryPolicy.LINK_DETACH);
                        .setTimeout(0L)
                        .setDynamic(false)
                        .setDynamicNodePropertiesNull()
                        .setDistributionModeNull()
                        .setFilterNull()
                        .getDefaultOutcome().setDeliveryState(Outcome.ACCEPTED).getComposite().maxLength(0).clear();
                attach.getTarget()
                        .setDescriptor()
                        .maxLength(255)
                        .setAddress(WRITE_UTF_8, targetAddress);
            }
            else
            {
                attach.getSource()
                        .setDescriptor()
                        .maxLength(255)
                        .setAddress(WRITE_UTF_8, sourceAddress)
                        .setDurable(TerminusDurability.NONE)
                        .setExpiryPolicy(TerminusExpiryPolicy.LINK_DETACH);

                attach.getTarget()
                        .setDescriptor()
                        .maxLength(255)
                        .setAddress(WRITE_UTF_8, targetAddress);

                attach.setUnsettledNull();
                attach.setIncompleteUnsettled(false);
                attach.setInitialDeliveryCount(0);

            }

            frame.bodyChanged();
            link.send(frame, attach);

            if(gatewayRole == Role.RECEIVER)
            {
                frame.setPerformative(FLOW);
                Flow flow = Flow.LOCAL_REF.get();
                flow.wrap(sender.getBuffer(), frame.bodyOffset(), true)
                        .clear()
                        .maxLength(255);
                flow.setNextIncomingId(0)
                        .setIncomingWindow(Integer.MAX_VALUE)
                        .setNextOutgoingId(1)
                        .setOutgoingWindow(0)
                        .setHandle(0)
                        .setDeliveryCount(0)
                        .setLinkCredit(1000);
                frame.bodyChanged();
                link.send(frame, flow);
            }



        }

        private static void whenDetachReceived(Link<AmqpLink> link, Frame frame, Detach detach)
        {
            //TODO(JAF): Need to remove the link from the producer/consumer map once it has been detached

            Sender sender = link.sender;
            frame.wrap(sender.getBuffer(), sender.getOffset(), true)
                    .setDataOffset(2)
                    .setType(0)
                    .setChannel(0)
                    .setPerformative(DETACH);
            detach.wrap(sender.getBuffer(), frame.bodyOffset(), true)
                    .maxLength(255)
                    .setHandle(0)
                    .setClosed(true);
            frame.bodyChanged();
            link.send(frame, detach);
        }

        private static void whenTransferReceived(Link<AmqpLink> link, Frame frame, Transfer transfer)
        {
            // TODO: This should settle only when required based on the incoming settled state
            Sender sender = link.sender;

            System.out.println("Received message transfer (handle " + transfer.getHandle() + ") on link (" + link + ")");
            //System.out.println("Original Buffer: " + toHex(sender.getBuffer().byteArray(), sender.getOffset(), 20));

            long handle = transfer.getHandle();
            long deliveryId = transfer.getDeliveryId();
            String deliveryTag = transfer.getDeliveryTag(READ_UTF_8);
            long format = transfer.getMessageFormat();
            boolean settled = transfer.getSettled();
            // send transfer to other attached session
            org.kaazing.nuklei.amqp_1_0.codec.messaging.Message message = transfer.getMessage();

            // get the string out
            String messageString = message.getValue(READ_UTF_8);
            //TODO(JAF): Don't allocate new byte arrays like this
            byte[] bytes = messageString.getBytes();
            MESSAGE.getBuffer().putBytes(0, bytes);
            MESSAGE.setOffset(0);
            MESSAGE.setLength(bytes.length);

            System.out.println("Sending message to aeron transport from amqp address: " + link.parameter.linkAddress);
            link.parameter.aeronTransportAdapter.onRemoteMessageReceived(link.parameter.linkAddress, MESSAGE);
            System.out.println("Message sent to aeron subscribers...");


            frame.wrap(sender.getBuffer(), sender.getOffset(), true)
                    .setDataOffset(2)
                    .setType(0)
                    .setChannel(0)
                    .setPerformative(DISPOSITION);
            Disposition disposition = Disposition.LOCAL_REF.get();
            disposition.wrap(sender.getBuffer(), frame.bodyOffset(), true)
                    .setRole(RECEIVER)
                    .setFirst(deliveryId)
                    .setLast(deliveryId)
                    .setSettled(true)
                    .getState().setDeliveryState(DeliveryState.ACCEPTED);
            frame.bodyChanged();
            link.send(frame, disposition);
        }
    }

    private static class AmqpProducer
    {
        private final Link<AmqpLink> link;
        private long handle;
        private final String address;
        public AmqpProducer(String address, Link<AmqpLink> link, long handle)
        {
            this.link = link;
            this.address = address;
            this.handle = handle;
        }
    }

    private static class AmqpConsumer
    {
        private final Link<AmqpLink> link;
        private final long handle;
        private final String address;
        private final AtomicLong deliveryCounter;

        public AmqpConsumer(String address, Link<AmqpLink> link, long handle)
        {
            this.link = link;
            this.address = address;
            this.handle = handle;
            deliveryCounter = new AtomicLong(1);
        }
    }

    private static class AmqpConnectionHooks extends ConnectionHooks<AmqpConnection, AmqpSession, AmqpLink>
    {
        public AmqpConnectionHooks()
        {
            whenHeaderReceived = AmqpConnectionHooks::whenHeaderReceived;
            whenOpenReceived = AmqpConnectionHooks::whenOpenReceived;
            whenCloseReceived = AmqpConnectionHooks::whenCloseReceived;
        }

        private static void whenHeaderReceived(Connection<AmqpConnection, AmqpSession, AmqpLink> connection,
                                        Header header)
        {
            Sender sender = connection.sender;
            header.wrap(sender.getBuffer(), sender.getOffset(), true)
                  .setProtocol(AMQP_PROTOCOL)
                  .setProtocolID(0x00)
                  .setMajorVersion(0x01)
                  .setMinorVersion(0x00)
                  .setRevisionVersion(0x00);

            connection.send(header);
        }

        private static void whenOpenReceived(Connection<AmqpConnection, AmqpSession, AmqpLink> connection,
                                      Frame frame,
                                      Open open)
        {
            // TODO: Create a connection to the Aeron bus?
            //       broker, is something similar needed here for the connection to the Aeron bus?
            // AmqpTestConnection parameter = connection.parameter;
            // parameter.connection = parameter.connectionFactory.createConnection();

            Sender sender = connection.sender;
            frame.wrap(sender.getBuffer(), sender.getOffset(), true)
                .setDataOffset(2)
                .setType(0)
                .setChannel(0)
                .setPerformative(OPEN);
            open.wrap(sender.getBuffer(), frame.bodyOffset(), true)
                .maxLength(255)
                .setContainerId(WRITE_UTF_8, "")
                .setHostname(WRITE_UTF_8, "")
                .setMaxFrameSize(1048576L);
            frame.bodyChanged();
            connection.send(frame, open);
        }

        private static void whenCloseReceived(Connection<AmqpConnection, AmqpSession, AmqpLink> connection,
                                              Frame frame,
                                              Close close)
        {
            // TODO:  close connection to Aeron bus?
            // AmqpTestConnection parameter = connection.parameter;
            // parameter.connection.close();

            Sender sender = connection.sender;
            frame.wrap(sender.getBuffer(), sender.getOffset(), true)
                  .setDataOffset(2)
                  .setType(0)
                  .setChannel(0)
                  .setPerformative(CLOSE);
            close.wrap(sender.getBuffer(), frame.bodyOffset(), true)
                 .maxLength(255)
                 .clear();
            frame.bodyChanged();
            connection.send(frame, close);
        }
    }

    private static class AmqpSessionHooks extends SessionHooks<AmqpSession, AmqpLink>
    {
        public AmqpSessionHooks()
        {
            whenBeginReceived = AmqpSessionHooks::whenBeginReceived;
            this.whenEndReceived = AmqpSessionHooks::whenEndReceived;
        }

        private static void whenBeginReceived(Session<AmqpSession, AmqpLink> session, Frame frame, Begin begin)
        {
            Sender sender = session.sender;
            frame.wrap(sender.getBuffer(), sender.getOffset(), true)
                    .setDataOffset(2)
                    .setType(0)
                    .setChannel(0)
                    .setPerformative(BEGIN);
            begin.wrap(sender.getBuffer(), frame.bodyOffset(), true) //.clear()
                    .maxLength(255)
                    .setRemoteChannel(0)
                    .setNextOutgoingId(1)
                    .setIncomingWindow(0)
                    .setOutgoingWindow(0)
                    .setHandleMax(1024);
            frame.bodyChanged();
            session.send(frame, begin);



            frame.setPerformative(FLOW);

            Flow flow = Flow.LOCAL_REF.get();
            flow.wrap(sender.getBuffer(), frame.bodyOffset(), true)
                    .clear()
                    .maxLength(255);
            flow.setNextIncomingId(0)
                    .setIncomingWindow(Integer.MAX_VALUE)
                    .setNextOutgoingId(1)
                    .setOutgoingWindow(0);
            frame.bodyChanged();
            session.send(frame, flow);
        }

        private static void whenEndReceived(Session<AmqpSession, AmqpLink> session, Frame frame, End end)
        {
            Sender sender = session.sender;
            frame.wrap(sender.getBuffer(), sender.getOffset(), true)
                    .setDataOffset(2)
                    .setType(0)
                    .setChannel(0)
                    .setPerformative(END);
            end.wrap(sender.getBuffer(), frame.bodyOffset(), true)
                    .clear();
            frame.bodyChanged();
            session.send(frame, end);
        }
    }

    private static final MutableDirectBufferMutator<String> WRITE_UTF_8 = newMutator(UTF_8);

    public static final MutableDirectBufferMutator<String> newMutator(final Charset charset)
    {
        return new MutableDirectBufferMutator<String>()
        {
            private final CharsetEncoder encoder = charset.newEncoder();
            private final int maxBytesPerChar = (int) encoder.maxBytesPerChar();

            @Override
            public int mutate(Mutation mutation, MutableDirectBuffer buffer, String value)
            {
                int offset = mutation.maxOffset(value.length() * maxBytesPerChar);
                ByteBuffer buf = buffer.byteBuffer();
                ByteBuffer out = buf != null ? buf.duplicate() : ByteBuffer.wrap(buffer.byteArray());
                out.position(offset);
                encoder.reset();
                encoder.encode(CharBuffer.wrap(value), out, true);
                encoder.flush(out);
                return out.position() - offset;
            }

        };
    }

    private static final DirectBufferAccessor<String> READ_UTF_8 = newAccessor(UTF_8);

    public static final DirectBufferAccessor<String> newAccessor(final Charset charset)
    {
        return new DirectBufferAccessor<String>()
        {
            private final CharsetDecoder decoder = charset.newDecoder();

            @Override
            public String access(DirectBuffer buffer, int offset, int size)
            {
                ByteBuffer buf = buffer.byteBuffer();
                ByteBuffer in = buf != null ? buf.duplicate() : ByteBuffer.wrap(buffer.byteArray());
                in.position(offset);
                in.limit(offset + size);
                CharBuffer out = CharBuffer.allocate(size);
                decoder.reset();
                decoder.decode(in, out, true);
                decoder.flush(out);
                out.flip();
                return out.toString();
            }
        };
    }


    private static class AmqpMessage implements CanonicalMessage
    {
        private UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(256));
        private int offset;
        private int length;

        @Override
        public UnsafeBuffer getBuffer()
        {
            return buffer;
        }

        @Override
        public int getOffset()
        {
            return offset;
        }

        public AmqpMessage setOffset(int offset)
        {
            this.offset = offset;
            return this;
        }

        @Override
        public int getLength()
        {
            return length;
        }

        public AmqpMessage setLength(int length)
        {
            this.length = length;
            return this;
        }
    }
}
