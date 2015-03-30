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
package org.kaazing.nuklei.amqp_1_0.codec.messaging;

import org.kaazing.nuklei.Flyweight;
import org.kaazing.nuklei.amqp_1_0.codec.types.CompositeMapType;
import uk.co.real_logic.agrona.DirectBuffer;

import java.util.function.Consumer;

/*
 * See AMQP 1.0 specification, section 3.5.3 "Source"
 */
public final class ApplicationProperties extends CompositeMapType.Described
{
    public ApplicationProperties()
    {
    }

    public ApplicationProperties setDescriptor()
    {
        super.setDescriptor(0x74L);
        return this;
    }

    @Override
    public ApplicationProperties watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public ApplicationProperties wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    @Override
    public ApplicationProperties maxLength(int value)
    {
        super.maxLength(value);
        return this;
    }

    @Override
    public ApplicationProperties maxCount(int value)
    {
        super.maxCount(value);
        return this;
    }
}
