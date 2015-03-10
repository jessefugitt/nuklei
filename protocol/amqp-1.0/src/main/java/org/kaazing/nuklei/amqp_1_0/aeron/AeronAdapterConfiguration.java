package org.kaazing.nuklei.amqp_1_0.aeron;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public class AeronAdapterConfiguration
{
    public static final String PROXY_CREATION_AT_STARTUP_PROP = "aeron.adapter.proxyCreationAtStartup";
    public static final String LINGER_TIMEOUT_MS_PROP = "aeron.adapter.lingerTimeout";
    public static final String EMBEDDED_MEDIA_DRIVER_PROP = "aeron.adapter.embeddedMediaDriver";
    public static final String FRAME_COUNT_LIMIT_PROP = "aeron.adapter.frameCountLimit";

    public static final boolean PROXY_CREATION_AT_STARTUP;
    public static final int FRAGMENT_COUNT_LIMIT;
    public static final long LINGER_TIMEOUT_MS;
    public static final boolean EMBEDDED_MEDIA_DRIVER;

    static
    {
        PROXY_CREATION_AT_STARTUP = Boolean.getBoolean(PROXY_CREATION_AT_STARTUP_PROP);
        FRAGMENT_COUNT_LIMIT = Integer.getInteger(FRAME_COUNT_LIMIT_PROP, 10);
        LINGER_TIMEOUT_MS = Long.getLong(LINGER_TIMEOUT_MS_PROP, TimeUnit.SECONDS.toMillis(5));
        EMBEDDED_MEDIA_DRIVER = Boolean.getBoolean(EMBEDDED_MEDIA_DRIVER_PROP);
    }

}
