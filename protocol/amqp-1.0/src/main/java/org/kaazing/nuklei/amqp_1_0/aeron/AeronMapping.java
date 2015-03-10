package org.kaazing.nuklei.amqp_1_0.aeron;

import java.util.List;
import java.util.Map;

/**
 * Interface to use to map to aeron streams.
 *
 */
public interface AeronMapping
{

    Map<String, List<AeronPhysicalStream>> getLogicalNameToSubscriptionsMap();

    Map<String, List<AeronPhysicalStream>> getLogicalNameToPublicationsMap();

    void loadPublication(String logicalName, AeronPhysicalStream physicalStream);

    void loadSubscription(String logicalName, AeronPhysicalStream physicalStream);

    List<AeronPhysicalStream> getPublications(String logicalName);

    List<AeronPhysicalStream> getSubscriptions(String logicalName);

}
