package org.kaazing.nuklei.amqp_1_0.aeron.mikro;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.kaazing.nuklei.amqp_1_0.codec.messaging.Performative.CLOSE;
import static org.kaazing.nuklei.amqp_1_0.codec.messaging.Performative.OPEN;
import static org.kaazing.nuklei.amqp_1_0.codec.transport.Header.AMQP_PROTOCOL;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.kaazing.nuklei.amqp_1_0.AmqpMikroFactory;
import org.kaazing.nuklei.amqp_1_0.aeron.AeronStaticTransportAdapter;
import org.kaazing.nuklei.amqp_1_0.aeron.AeronTransportAdapter;
import org.kaazing.nuklei.amqp_1_0.aeron.Message;
import org.kaazing.nuklei.amqp_1_0.codec.transport.*;
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

    private final BiConsumer<String, Message> aeronMessageHandler = new BiConsumer<String, Message>()
    {
        @Override
        public void accept(String logicalName, Message aeronMessage)
        {
            //TODO(JAF): This is where aeron messages will be passed from local publications
        }
    };

    private final AeronTransportAdapter aeronTransportAdapter = new AeronStaticTransportAdapter(aeronMessageHandler);
    private final Map<String, AmqpProducer> amqpProducerMap = new HashMap<String, AmqpProducer>();
    private final Map<String, AmqpConsumer> amqpConsumerMap = new HashMap<String, AmqpConsumer>();

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
        protected String attachLinkName;
        protected Map<String, AmqpProducer> producerMap;
        protected Map<String, AmqpConsumer> consumerMap;
        protected AeronTransportAdapter aeronTransportAdapter;
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
                            new SessionHooks<AmqpSession, AmqpLink>()));
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
                    new AmqpAeronLinkHooks()), localProducerMap, localConsumerMap, aeronTransportAdapter);
                    //new LinkHooks<AmqpTestLink>()));
        }
    }

    private static class AmqpAeronLinkHooks extends LinkHooks<AmqpLink>
    {
        private static final DirectBufferAccessor<String> READ_UTF_8 = newAccessor(UTF_8);

        public AmqpAeronLinkHooks()
        {
            whenAttachReceived = AmqpAeronLinkHooks::whenAttachReceived;
        }

        private static void whenAttachReceived(Link<AmqpLink> link, Frame frame, Attach attach)
        {
            AmqpLink parameter = link.parameter;
            //parameter.producer = parameter.session.createProducer(null);
            //parameter.attachLinkName = attach.getName(READ_UTF_8);
            //System.out.println("Attach received with name: " + parameter.attachLinkName);

            //String sourceAddress = attach.getSource().getAddress(READ_UTF_8);
            //System.out.println("Attach received with source address: " + sourceAddress);

            //String targetAddress = attach.getTarget().getAddress(READ_UTF_8);
            //System.out.println("Attach received with target address: " + targetAddress);

            parameter.attachLinkName = attach.getName(READ_UTF_8);
            System.out.println("Attach received with name: " + parameter.attachLinkName);

            //TODO(JAF): Figure out how to determine if this is a producer or consumer
            if(parameter.attachLinkName.indexOf("->") != -1)
            {
                AmqpConsumer localConsumer = new AmqpConsumer(parameter.attachLinkName);
                parameter.consumerMap.put(localConsumer.topicName, localConsumer);
                parameter.aeronTransportAdapter.onRemoteConsumerDetected(localConsumer.topicName);

            }
            else if(parameter.attachLinkName.indexOf("<-") != -1)
            {
                AmqpProducer localProducer = new AmqpProducer(parameter.attachLinkName);
                parameter.producerMap.put(localProducer.topicName, localProducer);
                parameter.aeronTransportAdapter.onRemoteProducerDetected(localProducer.topicName);

                //TODO(JAF): Remove the code that sends a fake message to ensure it is working
                /*
                final UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(256));
                final String text = "Hello World!";
                byte[] bytes = text.getBytes();
                buffer.putBytes(0, text.getBytes());
                final int offset = 0;
                final int length = bytes.length;
                Message testMessage = new Message()
                {
                    @Override
                    public DirectBuffer getBuffer()
                    {
                        return buffer;
                    }

                    @Override
                    public int getOffset()
                    {
                        return offset;
                    }

                    @Override
                    public int getLength()
                    {
                        return length;
                    }
                };

                //final boolean result = publication.offer(buffer, 0, message.getBytes().length);
                System.out.println("Sending initial message to aeron transport adapter on topic: " + localProducer.topicName);
                parameter.aeronTransportAdapter.onRemoteMessageReceived(localProducer.topicName, testMessage);
                */
            }

            Sender sender = link.sender;
            frame.wrap(sender.getBuffer(), sender.getOffset(), true)
                    .setDataOffset(2)
                    .setType(0)
                    .setChannel(0)
                    .setPerformative(OPEN);

            attach.wrap(sender.getBuffer(), frame.bodyOffset(), true)
                    .maxLength(255)
                    .clear();
            frame.bodyChanged();
            sender.send(frame.limit());

            //TODO: This isn't working to send OPEN back to the sender




            /* Old example
            sender.wrap(frame)
                    .setDataOffset(2)
                    .setType(0)
                    .setChannel(0)
                    .setPerformative(OPEN);
            attach.wrap(frame.buffer(), frame.bodyOffset(), false)
                    .maxLength(255)
                    .clear();
            */
            //connection.send(frame, open);

            //frame.bodyChanged();
            //sender.send(frame.limit());
        }
    }

    private static class AmqpProducer
    {
        private final String linkName;
        private final String topicName;
        public AmqpProducer(String linkName)
        {
            this.linkName = linkName;
            this.topicName = linkName.split("<-")[0];
        }
    }

    private static class AmqpConsumer
    {
        private final String linkName;
        private final String topicName;
        public AmqpConsumer(String linkName)
        {
            this.linkName = linkName;
            this.topicName = linkName.split("->")[0];
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
}
