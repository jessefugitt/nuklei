package org.kaazing.nuklei.amqp_1_0.aeron;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapping class used to build up the list of logical names to physical streams for Aeron.
 *
 */
public class AeronLogicalNameMapping
{
    private final Map<String, List<AeronPhysicalStream>> logicalToSubscriptionsMap = new HashMap<>();
    private final Map<String, List<AeronPhysicalStream>> logicalToPublicationsMap = new HashMap<>();

    public AeronLogicalNameMapping()
    {
    }

    public Map<String, List<AeronPhysicalStream>> getLogicalNameToSubscriptionsMap()
    {
        return logicalToSubscriptionsMap;
    }

    public Map<String, List<AeronPhysicalStream>> getLogicalNameToPublicationsMap()
    {
        return logicalToPublicationsMap;
    }

    public void loadPublication(String logicalName, AeronPhysicalStream physicalStream)
    {
        List<AeronPhysicalStream> publications = logicalToPublicationsMap.get(logicalName);
        if(publications == null)
        {
            publications = new ArrayList<AeronPhysicalStream>();
            List<AeronPhysicalStream> result = logicalToPublicationsMap.putIfAbsent(logicalName, publications);
            if(result != null)
            {
                //TODO(JAF): Throw an error that there is a race condition updating the map
            }
        }
        if(!publications.contains(physicalStream))
        {
            publications.add(physicalStream);
        }
    }

    public void loadSubscription(String logicalName, AeronPhysicalStream physicalStream)
    {
        List<AeronPhysicalStream> subscriptions = logicalToSubscriptionsMap.get(logicalName);
        if(subscriptions == null)
        {
            subscriptions = new ArrayList<AeronPhysicalStream>();
            List<AeronPhysicalStream> result = logicalToSubscriptionsMap.putIfAbsent(logicalName, subscriptions);
            if(result != null)
            {
                //TODO(JAF): Throw an error that there is a race condition updating the map
            }
        }
        if(!subscriptions.contains(physicalStream))
        {
            subscriptions.add(physicalStream);
        }
    }

    public List<AeronPhysicalStream> getPublications(String logicalName)
    {
        return logicalToPublicationsMap.get(logicalName);
    }

    public List<AeronPhysicalStream> getSubscriptions(String logicalName)
    {
        return logicalToSubscriptionsMap.get(logicalName);
    }
}
