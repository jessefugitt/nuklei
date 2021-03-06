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

import java.util.function.Consumer;

import org.kaazing.nuklei.Flyweight;
import org.kaazing.nuklei.amqp_1_0.codec.types.MapType;

import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 3.5.8 "Filter Set"
 */
public final class FilterSet extends MapType
{

    @Override
    public FilterSet watch(Consumer<Flyweight> notifier)
    {
        super.watch(notifier);
        return this;
    }

    @Override
    public FilterSet wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    public static final class Embedded<T> extends MapType
    {

        private final T parent;

        public Embedded(T parent)
        {
            this.parent = parent;
        }

        @Override
        public Embedded<T> watch(Consumer<Flyweight> notifier)
        {
            super.watch(notifier);
            return this;
        }

        @Override
        public Embedded<T> wrap(DirectBuffer buffer, int offset, boolean mutable)
        {
            super.wrap(buffer, offset, mutable);
            return this;
        }

        public T done()
        {
            return parent;
        }
    }
}
