package org.kaazing.nuklei.amqp_1_0.aeron.mikro;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.kaazing.nuklei.amqp_1_0.codec.messaging.Performative.CLOSE;
import static org.kaazing.nuklei.amqp_1_0.codec.messaging.Performative.OPEN;
import static org.kaazing.nuklei.amqp_1_0.codec.transport.Header.AMQP_PROTOCOL;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
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

    public AmqpAeronMikroSupport()
    {
    }

    public Mikro createAmqpAeronMikro()
    {
        final SenderFactory senderFactory = new TcpSenderFactory(new UnsafeBuffer(ByteBuffer.allocate(SEND_BUFFER_SIZE)));
        final ConnectionFactory<AmqpTestConnection, AmqpTestSession, AmqpTestLink> connectionFactory =
                new AmqpTestConnectionFactory();
        final ConnectionHandler<AmqpTestConnection, AmqpTestSession, AmqpTestLink> connectionHandler =
                new ConnectionHandler<AmqpTestConnection, AmqpTestSession, AmqpTestLink>(
                        new AmqpTestSessionFactory(),
                        new SessionHandler<AmqpTestSession, AmqpTestLink>(
                                new AmqpTestLinkFactory(),
                                new LinkHandler<AmqpTestLink>()));

        AmqpMikroFactory<AmqpTestConnection, AmqpTestSession, AmqpTestLink> factory =
                new AmqpMikroFactory<AmqpTestConnection, AmqpTestSession, AmqpTestLink>();
        return factory.newMikro(senderFactory, connectionFactory, connectionHandler);
    }

    private class AmqpTestConnection extends Connection<AmqpTestConnection, AmqpTestSession, AmqpTestLink>
    {
        // TODO: the JmsConnection stored a javax.jms.Connection object representing the connection to the
        //       broker, is something similar needed here for the connection to the Aeron bus?
        public AmqpTestConnection(ConnectionFactory<AmqpTestConnection, AmqpTestSession, AmqpTestLink> connectionFactory,
                                  ConnectionStateMachine<AmqpTestConnection, AmqpTestSession, AmqpTestLink> stateMachine,
                                  Sender sender, MutableDirectBuffer reassemblyBuffer)
        {
            super(stateMachine, sender, reassemblyBuffer);
            this.parameter = this;
        }
    }

    private class AmqpTestSession extends Session<AmqpTestSession, AmqpTestLink>
    {
        AmqpTestSession(Connection<AmqpTestConnection, AmqpTestSession, AmqpTestLink> owner,
                SessionStateMachine<AmqpTestSession, AmqpTestLink> stateMachine)
        {
            super(stateMachine, owner.sender);
            this.parameter = this;
        }
    }

    private class AmqpTestLink extends Link<AmqpTestLink>
    {
        String producerName;
        public AmqpTestLink(Session<AmqpTestSession, AmqpTestLink> owner,
                            LinkStateMachine<AmqpTestLink> stateMachine)
        {
            super(stateMachine, owner.sender);
            this.parameter = this;
        }
    }

    private class AmqpTestConnectionFactory implements ConnectionFactory<AmqpTestConnection, AmqpTestSession, AmqpTestLink>
    {
        @Override
        public Connection<AmqpTestConnection, AmqpTestSession, AmqpTestLink> newConnection(Sender sender,
                                                                                           MutableDirectBuffer reassemblyBuffer)
        {
            ConnectionStateMachine<AmqpTestConnection, AmqpTestSession, AmqpTestLink> stateMachine =
                    new ConnectionStateMachine<AmqpTestConnection, AmqpTestSession, AmqpTestLink>(
                            new AmqpTestConnectionHooks());
            return new AmqpTestConnection(this, stateMachine, sender, reassemblyBuffer);
        }
    }

    private class AmqpTestSessionFactory implements SessionFactory<AmqpTestConnection, AmqpTestSession, AmqpTestLink>
    {
        @Override
        public Session<AmqpTestSession, AmqpTestLink> newSession(
                Connection<AmqpTestConnection, AmqpTestSession, AmqpTestLink> connection)
        {
            return new AmqpTestSession(connection,
                    new SessionStateMachine<AmqpTestSession, AmqpTestLink>(
                            new SessionHooks<AmqpTestSession, AmqpTestLink>()));
        }
    }

    private class AmqpTestLinkFactory implements LinkFactory<AmqpTestSession, AmqpTestLink>
    {
        @Override
        public Link<AmqpTestLink> newLink(Session<AmqpTestSession, AmqpTestLink> session)
        {
            return new AmqpTestLink(session, new LinkStateMachine<AmqpTestLink>(
                    new AmqpAeronLinkHooks()));
                    //new LinkHooks<AmqpTestLink>()));
        }
    }

    private static class AmqpAeronLinkHooks extends LinkHooks<AmqpTestLink>
    {
        public AmqpAeronLinkHooks()
        {
            whenAttachReceived = AmqpAeronLinkHooks::whenAttachReceived;
        }

        private static void whenAttachReceived(Link<AmqpTestLink> link, Frame frame, Attach attach)
        {
            AmqpTestLink parameter = link.parameter;
            //parameter.producer = parameter.session.createProducer(null);

            //TODO(JAF): Fix this code to get the correct link name
            Sender sender = link.sender;
            parameter.producerName = attach.getName(new DirectBufferAccessor<String>()
            {
                @Override
                public String access(DirectBuffer buffer, int offset, int size)
                {
                    byte[] bytes = new byte[size];
                    buffer.getBytes(offset, bytes);
                    return new String(bytes);
                }
            });


            frame.wrap(sender.getBuffer(), sender.getOffset(), true)
                    .setDataOffset(2)
                    .setType(0)
                    .setChannel(0)
                    .setPerformative(OPEN);

            attach.wrap(frame.buffer(), frame.bodyOffset(), false)
                    .maxLength(255)
                    .clear();

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
            frame.bodyChanged();
            sender.send(frame.limit());
        }
    }

    private static class AmqpTestConnectionHooks extends ConnectionHooks<AmqpTestConnection, AmqpTestSession, AmqpTestLink>
    {
        public AmqpTestConnectionHooks()
        {
            whenHeaderReceived = AmqpTestConnectionHooks::whenHeaderReceived;
            whenOpenReceived = AmqpTestConnectionHooks::whenOpenReceived;
            whenCloseReceived = AmqpTestConnectionHooks::whenCloseReceived;
        }

        private static void whenHeaderReceived(Connection<AmqpTestConnection, AmqpTestSession, AmqpTestLink> connection,
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

        private static void whenOpenReceived(Connection<AmqpTestConnection, AmqpTestSession, AmqpTestLink> connection,
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

        private static void whenCloseReceived(Connection<AmqpTestConnection, AmqpTestSession, AmqpTestLink> connection,
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
}
