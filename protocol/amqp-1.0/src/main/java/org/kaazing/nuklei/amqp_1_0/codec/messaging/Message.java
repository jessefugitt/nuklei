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
import org.kaazing.nuklei.amqp_1_0.codec.types.Type;
import org.kaazing.nuklei.amqp_1_0.codec.types.ULongType;
import org.kaazing.nuklei.function.DirectBufferAccessor;
import org.kaazing.nuklei.function.MutableDirectBufferMutator;

import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, Part 3 "Messaging"
 */
public class Message extends Type
{
    public static final ThreadLocal<Message> LOCAL_REF = new ThreadLocal<Message>()
    {
        @Override
        protected Message initialValue()
        {
            return new Message();
        }
    };

    // header
    // delivery annotations
    // message annotations
    // properties
    // application properites
    // application data
    //     exactly one of:
    //         one or more DATA
    //         one or more AMQP Sequence
    //         one AMQP Value
    // footer
    private final Header header;
    //private final DeliveryAnnotations deliveryAnnotations;
    private final MessageAnnotations messageAnnotations;
    private final Properties properties;
    private final ApplicationProperties applicationProperties;

    // FIXME: there should be a kind() method that is driven by the descriptor, that
    //        method will determine whether Data, AmqpSequence, or AmqpValue is used,
    //        similar to how DynamicType works.
    private final ULongType.Descriptor descriptor;
//    private final Data data;
//    private final AmqpSequence sequences;
    private final AmqpValue value;
//    private final Footer footer;

    private int limit;

    private boolean payloadOnly;

    public Message()
    {
        payloadOnly = false;

        header = new Header().watch((owner) ->
        {
            limit(owner.limit());
        });
//        deliveryAnnotations = new DeliveryAnnotations().watch((owner) ->
//        {
//            limit(2, owner.limit());
//        });

        messageAnnotations = new MessageAnnotations().watch((owner) ->
        {
            limit(owner.limit());
        });

        properties = new Properties().watch((owner) ->
        {
            limit(owner.limit());
        });

        applicationProperties = new ApplicationProperties().watch((owner) ->
        {
            limit(owner.limit());
        });

        descriptor = new ULongType.Descriptor().watch((owner) ->
        {
            limit(owner.limit());
        });

//        data = new Data().watch((owner) ->
//        {
//            limit(6, owner.limit());
//        });
//        sequences = new AmqpSequence().watch((owner) ->
//        {
//            limit(7, owner.limit());
//        });

        value = new AmqpValue().watch((owner) ->
        {
//            limit(8, owner.limit());
            limit(owner.limit());
        });
//        footer = new Footer().watch((owner) ->
//        {
//            limit(9, owner.limit());
//        });
    }

    //TODO(JAF): This is a quick approach to getting payload only messages to work while still implementing full parsing
    public Message setPayloadOnly(boolean payloadOnly)
    {
        this.payloadOnly = payloadOnly;
        return this;
    }

    public boolean isPayloadOnly()
    {
        return payloadOnly;
    }

    @Override
    public Kind kind()
    {
        return Kind.MESSAGE;
    }

    @Override
    public Message watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public Message wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    protected void limit(int limit)
    {
        this.limit = limit;
        notifyChanged();
    }

    @Override
    public int limit()
    {
        return limit;
    }

    public Header getHeader()
    {
        return header();
    }

    public MessageAnnotations getMessageAnnotations()
    {
        return messageAnnotations();
    }

    public Properties getProperties()
    {
        return properties();
    }

    public ApplicationProperties getApplicationProperties()
    {
        return applicationProperties();
    }

    public Message setDescriptor(long code)
    {
        descriptor().set(code);
        return this;
    }

    public <T> Message setValue(MutableDirectBufferMutator<T> mutator, T value)
    {
        value().setValue(mutator, value);
        return this;
    }

    public <T> T getValue(DirectBufferAccessor<T> accessor)
    {
        return value().getValue(accessor);
    }

    private Header header()
    {
        return header.wrap(buffer(), offset(), true);
    }

    private MessageAnnotations messageAnnotations()
    {
        return messageAnnotations.wrap(buffer(), header().limit(), true);
    }

    private Properties properties()
    {
        return properties.wrap(buffer(), messageAnnotations().limit(), true);
    }

    private ApplicationProperties applicationProperties()
    {
        return applicationProperties.wrap(buffer(), properties().limit(), true);
    }

    private ULongType.Descriptor descriptor()
    {
        if(isPayloadOnly())
        {
            return descriptor.wrap(buffer(), offset(), true);
        }
        else
        {
            //return descriptor.wrap(buffer(), applicationProperties().limit(), true); //legacty qpid jms client
            return descriptor.wrap(buffer(), properties().limit(), true); //new qpid jms client
        }
    }

    private AmqpValue value()
    {
        return value.wrap(buffer(), descriptor().limit(), true);
    }
}
