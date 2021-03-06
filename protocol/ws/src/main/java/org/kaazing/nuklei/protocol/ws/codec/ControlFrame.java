/*
 * Copyright 2015 Kaazing Corporation, All rights reserved.
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
package org.kaazing.nuklei.protocol.ws.codec;

import static java.lang.String.format;

import uk.co.real_logic.agrona.DirectBuffer;

public abstract class ControlFrame extends Frame
{

    ControlFrame()
    {

    }

    @Override
    protected ControlFrame wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        if (!isFin())
        {
            protocolError(format("Expected FIN for %s frame", getOpCode()));
        }
        return this;
    }

    @Override
    protected int getMaxPayloadLength()
    {
        return 125;
    }

}
