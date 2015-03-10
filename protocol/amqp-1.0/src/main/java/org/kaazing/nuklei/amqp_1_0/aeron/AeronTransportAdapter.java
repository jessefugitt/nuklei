package org.kaazing.nuklei.amqp_1_0.aeron;

/**
 * Defines interface needed to be a transport adapter in the gateway.
 * The main abstractions are:
 * Local producers/consumers that are connected to this specific transport
 * Remote producers/consumers that are connected via a different transport (but will cause events to flow to/from this transport)
 * Proxy producers/consumers that are created internally and logically represent the remote producers/consumers
 *
 * Typical implementations will do the following in response to events:
 * When a remote producer is detected, create a proxy producer
 * When a remote consumer is detected, create a proxy consumer
 * When a local producer is detected, connect any corresponding proxy consumers to that local producer
 * When a local consumer is detected, connect any corresponding proxy producers to that local consumer
 *  (proxy creation can event driven based on events from remote producers/consumers or done statically at startup if desired)
 *
 * Also, if a transport implementation lacks the ability to dynamically detect local producers/consumers,
 * a combination of a static proxy configuration combined with corresponding configuration provided to local producers/consumers
 * can be an alternative to dynamic detection of local producers/consumers and eliminate the need to connect proxy to local.
 */
public interface AeronTransportAdapter
{

    void onRemoteProducerDetected(String logicalName);
    void onRemoteProducerRemoved(String logicalName);
    void onRemoteConsumerDetected(String logicalName);
    void onRemoteConsumerRemoved(String logicalName);

    void onLocalProducerDetected(AeronPhysicalStream physicalStream);
    void onLocalProducerRemoved(AeronPhysicalStream physicalStream);
    void onLocalConsumerDetected(AeronPhysicalStream physicalStream);
    void onLocalConsumerRemoved(AeronPhysicalStream physicalStream);

    void onRemoteMessageReceived(String logicalName, Message message);
    void onLocalMessageReceived(String logicalName, AeronPhysicalStream physicalStream, AeronMessage message);

    void start();
    void stop();
}
