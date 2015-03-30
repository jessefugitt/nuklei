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
import org.kaazing.nuklei.amqp_1_0.codec.definitions.Optional;
import org.kaazing.nuklei.amqp_1_0.codec.types.*;
import uk.co.real_logic.agrona.DirectBuffer;

import java.util.function.Consumer;

/*
 * See AMQP 1.0 specification, section 3.5.3 "Source"
 */
public final class Header extends CompositeType.Described
{
    private final Optional<BooleanType> durableField;
    private final Optional<UByteType> priorityField;
    //ttl
    //first-acquirer
    //delivery-count


    public Header()
    {
        BooleanType durable = new BooleanType().watch((owner) ->
        {
            limit(1, owner.limit());
        });
        durableField = new Optional<>(durable).watch((owner) ->
        {
            limit(1, owner.limit());
        });

        UByteType priority = new UByteType().watch((owner) ->
        {
            limit(2, owner.limit());
        });
        priorityField = new Optional<>(priority).watch((owner) ->
        {
            limit(2, owner.limit());
        });
    }

    public Header setDescriptor()
    {
        super.setDescriptor(0x70L);
        return this;
    }

    @Override
    public Header watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public Header wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    @Override
    public Header maxLength(int value)
    {
        super.maxLength(value);
        return this;
    }

    @Override
    public Header maxCount(int value)
    {
        super.maxCount(value);
        return this;
    }


    public boolean isDurableNull()
    {
        return durable().isNull();
    }

    public Header setDurableNull()
    {
        durable().setNull();
        return this;
    }

    public Header setDurable(boolean value)
    {
        durable().getType().set(value);
        return this;
    }

    public boolean getDurable()
    {
        return durable().getType().get();
    }


    public boolean isPriorityNull()
    {
        return priority().isNull();
    }

    public Header setPriorityNull()
    {
        priority().setNull();
        return this;
    }

    public Header setPriority(short value)
    {
        priority().getType().set(value);
        return this;
    }

    public int getPriority()
    {
        return priority().getType().get();
    }

    private Optional<UByteType> priority()
    {
        return priorityField.wrap(mutableBuffer(), offsetBody(), true);
    }

    private Optional<BooleanType> durable()
    {
        return durableField.wrap(mutableBuffer(), priority().limit(), true);
    }

}
