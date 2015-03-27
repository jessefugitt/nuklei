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
import org.kaazing.nuklei.amqp_1_0.codec.definitions.Optional;
import org.kaazing.nuklei.amqp_1_0.codec.types.ArrayType;
import org.kaazing.nuklei.amqp_1_0.codec.types.BooleanType;
import org.kaazing.nuklei.amqp_1_0.codec.types.CompositeType;
import org.kaazing.nuklei.amqp_1_0.codec.types.StringType;
import org.kaazing.nuklei.amqp_1_0.codec.types.SymbolType;
import org.kaazing.nuklei.amqp_1_0.codec.types.UIntType;
import org.kaazing.nuklei.function.DirectBufferAccessor;
import org.kaazing.nuklei.function.MutableDirectBufferMutator;

import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 3.5.3 "Source"
 */
public final class Source extends CompositeType.Described
{
    private final StringType address;
    private final Optional<UIntType> durableField;
    //private final UIntType durable;
    private final Optional<SymbolType> expiryPolicyField;
    //private final SymbolType expiryPolicy;
    private final Optional<UIntType> timeoutField;
    //private final UIntType timeout;
    private final Optional<BooleanType> dynamicField;
    //private final BooleanType dynamic;
    private final Optional<NodeProperties> dynamicNodePropertiesField;
    //private final NodeProperties dynamicNodeProperties;
    private final Optional<SymbolType> distributionModeField;
    //private final SymbolType distributionMode;
    private final Optional<FilterSet.Embedded<Source>> filterField;
    //private final FilterSet.Embedded<Source> filter;
    private final Optional<Outcome.Described> defaultOutcomeField;
    //private final Outcome.Described defaultOutcome;
    private final ArrayType outcomes;
    private final ArrayType capabilities;

    public Source()
    {
        address = new StringType().watch((owner) ->
        {
            limit(1, owner.limit());
        });
        UIntType durable = new UIntType().watch((owner) ->
        {
            limit(2, owner.limit());
        });
        durableField = new Optional<>(durable).watch((owner) ->
        {
            limit(2, owner.limit());
        });

        SymbolType expiryPolicy = new SymbolType().watch((owner) ->
        {
            limit(3, owner.limit());
        });
        expiryPolicyField = new Optional<>(expiryPolicy).watch((owner) ->
        {
            limit(3, owner.limit());
        });

        UIntType timeout = new UIntType().watch((owner) ->
        {
            limit(4, owner.limit());
        });
        timeoutField = new Optional<>(timeout).watch((owner) ->
        {
            limit(4, owner.limit());
        });

        BooleanType dynamic = new BooleanType().watch((owner) ->
        {
            limit(5, owner.limit());
        });
        dynamicField = new Optional<>(dynamic).watch((owner) ->
        {
            limit(5, owner.limit());
        });

        NodeProperties dynamicNodeProperties = new NodeProperties().watch((owner) ->
        {
            limit(6, owner.limit());
        });
        dynamicNodePropertiesField = new Optional<>(dynamicNodeProperties).watch((owner) ->
        {
            limit(6, owner.limit());
        });

        SymbolType distributionMode = new SymbolType().watch((owner) ->
        {
            limit(7, owner.limit());
        });
        distributionModeField = new Optional<>(distributionMode).watch((owner) ->
        {
            limit(7, owner.limit());
        });

        FilterSet.Embedded<Source> filter = new FilterSet.Embedded<>(this).watch((owner) ->
        {
            limit(8, owner.limit());
        });
        filterField = new Optional<>(filter).watch((owner) ->
        {
            limit(8, owner.limit());
        });

        Outcome.Described defaultOutcome = new Outcome.Described().watch((owner) ->
        {
            limit(9, owner.limit());
        });
        defaultOutcomeField = new Optional<>(defaultOutcome).watch((owner) ->
        {
            limit(9, owner.limit());
        });

        outcomes = new ArrayType().watch((owner) ->
        {
            limit(10, owner.limit());
        });
        capabilities = new ArrayType().watch((owner) ->
        {
            limit(11, owner.limit());
        });
    }

    public Source setDescriptor()
    {
        super.setDescriptor(0x28L);
        return this;
    }

    @Override
    public Source watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public Source wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    @Override
    public Source maxLength(int value)
    {
        super.maxLength(value);
        return this;
    }

    @Override
    public Source maxCount(int value)
    {
        super.maxCount(value);
        return this;
    }

    public <T> Source setAddress(MutableDirectBufferMutator<T> mutator, T value)
    {
        address().set(mutator, value);
        return this;
    }

    public <T> T getAddress(DirectBufferAccessor<T> accessor)
    {
        return address().get(accessor);
    }

    public boolean isDurableNull()
    {
        return durable().isNull();
    }

    public Source setDurableNull()
    {
        durable().setNull();
        return this;
    }

    public Source setDurable(TerminusDurability value)
    {
        durable().getType().set(TerminusDurability.WRITE, value);
        return this;
    }

    public TerminusDurability getDurable()
    {
        return durable().getType().get(TerminusDurability.READ);
    }

    public boolean isExpiryPolicyNull()
    {
        return expiryPolicy().isNull();
    }

    public Source setExpiryPolicyNull()
    {
        expiryPolicy().setNull();
        return this;
    }

    public Source setExpiryPolicy(TerminusExpiryPolicy value)
    {
        expiryPolicy().getType().set(TerminusExpiryPolicy.WRITE, value);
        return this;
    }

    public TerminusExpiryPolicy getExpiryPolicy()
    {
        return expiryPolicy().getType().get(TerminusExpiryPolicy.READ);
    }

    public boolean isTimeoutNull()
    {
        return timeout().isNull();
    }

    public Source setTimeoutNull()
    {
        timeout().setNull();
        return this;
    }

    public Source setTimeout(long value)
    {
        timeout().getType().set(value);
        return this;
    }

    public long getTimeout()
    {
        return timeout().getType().get();
    }

    public boolean isDynamicNull()
    {
        return dynamic().isNull();
    }

    public Source setDynamicNull()
    {
        dynamic().setNull();
        return this;
    }

    public Source setDynamic(boolean value)
    {
        dynamic().getType().set(value);
        return this;
    }

    public boolean getDynamic()
    {
        return dynamic().getType().get();
    }

    public boolean isDynamicNodePropertiesNull()
    {
        return dynamicNodeProperties().isNull();
    }

    public Source setDynamicNodePropertiesNull()
    {
        dynamicNodeProperties().setNull();
        return this;
    }

    public NodeProperties getDynamicNodeProperties()
    {
        return dynamicNodeProperties().getType();
    }

    public boolean isDistributionModeNull()
    {
        return distributionMode().isNull();
    }

    public Source setDistributionModeNull()
    {
        distributionMode().setNull();
        return this;
    }

    public Source setDistributionMode(DistributionMode value)
    {
        distributionMode().getType().set(DistributionMode.WRITE, value);
        return this;
    }

    public DistributionMode getDistributionMode()
    {
        return distributionMode().getType().get(DistributionMode.READ);
    }

    public boolean isFilterNull()
    {
        return filter().isNull();
    }

    public Source setFilterNull()
    {
        filter().setNull();
        return this;
    }

    public FilterSet.Embedded<Source> getFilter()
    {
        return filter().getType();
    }

    public boolean isDefaultOutcomeNull()
    {
        return defaultOutcome().isNull();
    }

    public Source setDefaultOutcomeNull()
    {
        defaultOutcome().setNull();
        return this;
    }

    public Outcome.Described getDefaultOutcome()
    {
        return defaultOutcome().getType();
    }

    public Source setOutcomes(ArrayType value)
    {
        outcomes().set(value);
        return this;
    }

    public ArrayType getOutcomes()
    {
        return outcomes();
    }

    public Source setCapabilities(ArrayType value)
    {
        capabilities().set(value);
        return this;
    }

    public ArrayType getCapabilities()
    {
        return capabilities();
    }

    private StringType address()
    {
        return address.wrap(mutableBuffer(), offsetBody(), true);
    }

    private Optional<UIntType> durable()
    {
        return durableField.wrap(mutableBuffer(), address().limit(), true);
    }

    private Optional<SymbolType> expiryPolicy()
    {
        return expiryPolicyField.wrap(mutableBuffer(), durable().limit(), true);
    }

    private Optional<UIntType> timeout()
    {
        return timeoutField.wrap(mutableBuffer(), expiryPolicy().limit(), true);
    }

    private Optional<BooleanType> dynamic()
    {
        return dynamicField.wrap(mutableBuffer(), timeout().limit(), true);
    }

    private Optional<NodeProperties> dynamicNodeProperties()
    {
        return dynamicNodePropertiesField.wrap(mutableBuffer(), dynamic().limit(), true);
    }

    private Optional<SymbolType> distributionMode()
    {
        return distributionModeField.wrap(mutableBuffer(), dynamicNodeProperties().limit(), true);
    }

    private Optional<FilterSet.Embedded<Source>> filter()
    {
        return filterField.wrap(mutableBuffer(), distributionMode().limit(), true);
    }

    private Optional<Outcome.Described> defaultOutcome()
    {
        return defaultOutcomeField.wrap(mutableBuffer(), filter().limit(), true);
    }

    private ArrayType outcomes()
    {
        return outcomes.wrap(mutableBuffer(), defaultOutcome().limit(), true);
    }

    private ArrayType capabilities()
    {
        return capabilities.wrap(mutableBuffer(), outcomes().limit(), true);
    }
}
