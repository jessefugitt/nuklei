package org.kaazing.nuklei.kompound;

import org.kaazing.nuklei.amqp_1_0.aeron.mikro.AmqpAeronMikroSupport;
import org.kaazing.nuklei.function.Mikro;
import org.kaazing.nuklei.kompound.cmd.StopCmd;
import org.kaazing.nuklei.protocol.tcp.TcpManagerTypeId;

import java.util.concurrent.atomic.AtomicBoolean;

public class KompoundAmqpAeronMain
{
    //These should be added to the kaazing gateway
    //<env.java.naming.factory.initial>org.apache.qpid.jms.jndi.JmsInitialContextFactory</env.java.naming.factory.initial>
    //<env.connectionfactory.ConnectionFactory>amqp://kompoundamqpIP:5672?amqp.saslLayer=false</env.connectionfactory

    public static final String URI = "tcp://0.0.0.0:5672";

    public static void main(String[] args) throws Exception
    {
        AtomicBoolean attached = new AtomicBoolean(false);
        Kompound kompound;
        AmqpAeronMikroSupport amqpAeronMikroSupport = new AmqpAeronMikroSupport(
                //AmqpAeronMikroSupport.ExpectedMessageLayout.HEADER_ANNOTATION_PROPERTIES_APROPERTIES_PAYLOAD_FOOTER);
                AmqpAeronMikroSupport.ExpectedMessageLayout.HEADER_ANNOTATION_PROPERTIES_PAYLOAD);
        Mikro mikro = amqpAeronMikroSupport.createAmqpAeronMikro();

        final Kompound.Builder builder = new Kompound.Builder()
                .service(
                        URI,
                        (header, typeId, buffer, offset, length) ->
                        {
                            switch (typeId)
                            {
                                case TcpManagerTypeId.ATTACH_COMPLETED:
                                    attached.lazySet(true);
                                    break;
                                case TcpManagerTypeId.NEW_CONNECTION:
                                case TcpManagerTypeId.RECEIVED_DATA:
                                case TcpManagerTypeId.EOF:
                                    mikro.onMessage(header, typeId, buffer, offset, length);
                                    break;
                                case TcpManagerTypeId.NONE:
                                    if (header instanceof StopCmd)
                                    {
                                        amqpAeronMikroSupport.close();
                                    }
                                    break;
                            }
                        });

        kompound = Kompound.startUp(builder);
        while (!attached.get())
        {
            Thread.sleep(10);
        }

        System.out.println("AMQP Aeron Kompound running...");
        while (true)
        {
            Thread.sleep(1000);
        }
    }
}
