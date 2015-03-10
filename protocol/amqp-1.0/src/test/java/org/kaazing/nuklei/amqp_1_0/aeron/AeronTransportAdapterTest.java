package org.kaazing.nuklei.amqp_1_0.aeron;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Properties;
import java.util.function.BiConsumer;

import org.junit.Before;
import org.junit.Test;

import uk.co.real_logic.aeron.Publication;

public class AeronTransportAdapterTest
{
    private AeronStaticTransportAdapter aeronStaticTransportAdapter;
    @Before
    public void setUp()
    {
        BiConsumer<String, Message> testMessageHandler = new BiConsumer<String, Message>()
        {
            @Override
            public void accept(String logicalName, Message message)
            {
            }
        };
        aeronStaticTransportAdapter = new AeronStaticTransportAdapter(testMessageHandler);
        aeronStaticTransportAdapter.aeronWrapper = mock(AeronStaticTransportAdapter.AeronWrapper.class);
    }
    @Test
    public void shouldInitializeInStart()
    {
        //connectionHooks.whenInitialized = mock(Consumer.class);

        //stateMachine.start(connection);

        //assertSame(ConnectionState.START, connection.state);

        //verify(connectionHooks.whenInitialized).accept(connection);
    }
    @Test
    public void testLoadPropertiesFromFile() throws IOException
    {
        //topic.ABC = udp://localhost:30123,10
        //topic.DEF = udp://localhost:30123,11
        String fileName = "test_publications.properties";
        Properties testProps = aeronStaticTransportAdapter.loadPropertiesFromFile(fileName);
        assertEquals("udp://localhost:30123,10", testProps.getProperty("topic.ABC"));
        assertEquals("udp://localhost:30123,11", testProps.getProperty("topic.DEF"));
    }

    @Test
    public void testAddDeleteProxyPublication()
    {

        AeronPhysicalStream physicalStream = new AeronPhysicalStream("udp://localhost:30123,10", 99);
        Publication mockPublication = mock(Publication.class);
        when(mockPublication.channel()).thenReturn(physicalStream.getChannel());
        when(mockPublication.streamId()).thenReturn(physicalStream.getStream());
        when(aeronStaticTransportAdapter.aeronWrapper.addPublication(physicalStream.getChannel(), physicalStream.getStream())
                ).thenReturn(mockPublication);

        aeronStaticTransportAdapter.addProxyPublication("topic.test.1", physicalStream);
        verify(aeronStaticTransportAdapter.aeronWrapper).addPublication(physicalStream.getChannel(), physicalStream.getStream());

        Publication proxyPublication = aeronStaticTransportAdapter.deleteProxyPublication(physicalStream);
        assertNotNull(proxyPublication);
        assertEquals(physicalStream.getChannel(), proxyPublication.channel());
        assertEquals(physicalStream.getStream(), proxyPublication.streamId());
        assertNull(aeronStaticTransportAdapter.deleteProxyPublication(physicalStream));
    }

    @Test
    public void testOnRemoteMessageReceived()
    {
        String logicalName = "topic.test.1";
        AeronPhysicalStream physicalStream = new AeronPhysicalStream("udp://localhost:30123,10", 99);
        aeronStaticTransportAdapter.logicalNameMapping.loadPublication(logicalName, physicalStream);

        Publication mockPublication = mock(Publication.class);
        when(mockPublication.channel()).thenReturn(physicalStream.getChannel());
        when(mockPublication.streamId()).thenReturn(physicalStream.getStream());
        when(aeronStaticTransportAdapter.aeronWrapper.addPublication(physicalStream.getChannel(), physicalStream.getStream())
                ).thenReturn(mockPublication);

        aeronStaticTransportAdapter.onRemoteProducerDetected(logicalName);
        verify(aeronStaticTransportAdapter.aeronWrapper).addPublication(physicalStream.getChannel(), physicalStream.getStream());

        Message mockMessage = mock(Message.class);
        aeronStaticTransportAdapter.onRemoteMessageReceived(logicalName, mockMessage);

        verify(mockPublication).offer(mockMessage.getBuffer(), mockMessage.getOffset(), mockMessage.getLength());

    }

}
