/*
 * Copyright 2014 Kaazing Corporation, All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaazing.nuklei.kompound.cmd;

import org.kaazing.nuklei.kompound.MikroLocator;

import java.util.Map;

/**
 * Command sent to inform {@link org.kaazing.nuklei.function.Mikro} of start up, configurationMap, etc.
 */
public class StartCmd
{
    private MikroLocator locator;
    private Map<String, Object> configurationMap;

    public void reset(final MikroLocator locator, final Map<String, Object> configurationMap)
    {
        this.locator = locator;
        this.configurationMap = configurationMap;
    }

    public MikroLocator locator()
    {
        return locator;
    }

    public Map<String, Object> configurationMap()
    {
        return configurationMap;
    }
}
