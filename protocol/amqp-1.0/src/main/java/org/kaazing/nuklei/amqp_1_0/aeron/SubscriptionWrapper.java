package org.kaazing.nuklei.amqp_1_0.aeron;

import uk.co.real_logic.aeron.Subscription;
import uk.co.real_logic.aeron.logbuffer.FragmentHandler;

public class SubscriptionWrapper
{
    private final Subscription subscription;
    private final FragmentHandler dataHandler;

    public SubscriptionWrapper(Subscription subscription, FragmentHandler dataHandler)
    {
        this.subscription = subscription;
        this.dataHandler = dataHandler;
    }

    public Subscription getSubscription()
    {
        return subscription;
    }

    public FragmentHandler getDataHandler()
    {
        return dataHandler;
    }
}
