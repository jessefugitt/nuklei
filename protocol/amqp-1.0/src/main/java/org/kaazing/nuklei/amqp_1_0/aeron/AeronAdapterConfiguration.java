package org.kaazing.nuklei.amqp_1_0.aeron;

import uk.co.real_logic.aeron.common.CommonContext;
import static java.lang.System.setProperty;

/**
 *
 */
public class AeronAdapterConfiguration
{
    public static final String PROXY_CREATION_AT_STARTUP_PROP = "aeron.adapter.proxyCreationAtStartup";
    public static final String FRAME_COUNT_LIMIT_PROP = "aeron.adapter.frameCountLimit";
    public static final String DISABLE_EMBEDDED_MEDIA_DRIVER_PROP = "aeron.adapter.disableEmbeddedMediaDriver";
    public static final String DISABLE_DELETE_DIRS_ON_EXIT_PROP = "aeron.adapter.disableDeleteDirsOnExit";

    public static final boolean PROXY_CREATION_AT_STARTUP;
    public static final int FRAGMENT_COUNT_LIMIT;
    public static final boolean DISABLE_EMBEDDED_MEDIA_DRIVER;
    public static final boolean DISABLE_DELETE_DIRS_ON_EXIT;

    static
    {
        PROXY_CREATION_AT_STARTUP = Boolean.getBoolean(PROXY_CREATION_AT_STARTUP_PROP);
        FRAGMENT_COUNT_LIMIT = Integer.getInteger(FRAME_COUNT_LIMIT_PROP, 10);
        DISABLE_EMBEDDED_MEDIA_DRIVER = Boolean.getBoolean(DISABLE_EMBEDDED_MEDIA_DRIVER_PROP);
        DISABLE_DELETE_DIRS_ON_EXIT = Boolean.getBoolean(DISABLE_DELETE_DIRS_ON_EXIT_PROP);

        //Set to delete dirs on exit
        if(DISABLE_DELETE_DIRS_ON_EXIT)
        {
            setProperty(CommonContext.DIRS_DELETE_ON_EXIT_PROP_NAME, "false");
        }
        else
        {
            setProperty(CommonContext.DIRS_DELETE_ON_EXIT_PROP_NAME, "true");
        }
    }

}
