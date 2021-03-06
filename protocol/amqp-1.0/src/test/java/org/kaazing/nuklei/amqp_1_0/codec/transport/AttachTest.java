package org.kaazing.nuklei.amqp_1_0.codec.transport;

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

import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.kaazing.nuklei.amqp_1_0.codec.definitions.ReceiverSettleMode;
import org.kaazing.nuklei.amqp_1_0.codec.definitions.Role;
import org.kaazing.nuklei.amqp_1_0.codec.definitions.SenderSettleMode;
import org.kaazing.nuklei.amqp_1_0.codec.messaging.*;
import org.kaazing.nuklei.function.DirectBufferAccessor;
import org.kaazing.nuklei.function.MutableDirectBufferMutator;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.util.Random;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.kaazing.nuklei.amqp_1_0.codec.util.FieldAccessors.newAccessor;
import static org.kaazing.nuklei.amqp_1_0.codec.util.FieldMutators.newMutator;
import static uk.co.real_logic.agrona.BitUtil.fromHex;
import static uk.co.real_logic.agrona.BitUtil.toHex;

@RunWith(Theories.class)
public class AttachTest
{

    private static final int BUFFER_CAPACITY = 1024;
    private static final DirectBufferAccessor<String> READ_UTF_8 = newAccessor(UTF_8);
    private static final MutableDirectBufferMutator<String> WRITE_UTF_8 = newMutator(UTF_8);

    @DataPoint
    public static final int ZERO_OFFSET = 0;

    @DataPoint
    public static final int NON_ZERO_OFFSET = new Random().nextInt(BUFFER_CAPACITY - 512) + 1;

    private final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[BUFFER_CAPACITY]);

    @Theory
    public void shouldDecodeFromProducer(int offset)
    {
        //Example from QPID JMS 0.28 Producer connecting to a broker
        buffer.putBytes(offset, fromHex("c0ad0aa12f746f7069632e4142433c2d63643936396465612d393939382d343931342d62396265" +
            "2d613664323562343736363031434250005000005328c05c0aa12463643936396465612d393939382d343931342d623962652d6136" +
            "643235623437363630314040404040404000532445e02802a312616d71703a61636365707465643a6c69737412616d71703a72656a" +
            "65637465643a6c697374005329c00c01a109746f7069632e414243404043"));

        Attach attach = new Attach();
        attach.wrap(buffer, offset, true);

        assertEquals("topic.ABC<-cd969dea-9998-4914-b9be-a6d25b476601", attach.getName(READ_UTF_8));
        assertEquals(0L, attach.getHandle());
        assertEquals(Role.SENDER, attach.getRole());
        assertEquals(SenderSettleMode.UNSETTLED, attach.getSendSettleMode());
        assertEquals(ReceiverSettleMode.FIRST, attach.getReceiveSettleMode());

        Source source = attach.getSource();
        assertNotNull(source);

        //10 fields in the source
        assertEquals(10, source.getComposite().count());
        assertEquals("cd969dea-9998-4914-b9be-a6d25b476601", source.getAddress(READ_UTF_8));
        assertTrue(source.isDurableNull());
        assertTrue(source.isExpiryPolicyNull());
        assertTrue(source.isTimeoutNull());
        assertTrue(source.isDynamicNull());
        assertTrue(source.isDynamicNodePropertiesNull());
        assertTrue(source.isDistributionModeNull());
        assertTrue(source.isFilterNull());
        assertEquals(Outcome.ACCEPTED, source.getDefaultOutcome().getOutcome());
        assertTrue(source.getOutcomes().count() == 2);

        Target target = attach.getTarget();
        assertNotNull(target);

        //1 field in target
        assertEquals(1, target.getComposite().count());
        assertEquals("topic.ABC", target.getAddress(READ_UTF_8));

    }

    @Theory
    public void shouldDecodeFromConsumer(int offset)
    {
        //Example from QPID JMS 0.28 Consumer connecting to a broker
        buffer.putBytes(offset, fromHex("c08607a132746f7069632e4142432d3e202832386164363961342d366166362d343235332d6135" +
                "63392d37623435366632393436323529434150005000005328c01a03a109746f7069632e41424343a30b6c696e6b2d64657461" +
                "6368005329c02701a12435376630656631362d666337612d343666362d386264342d643265656635353433356138"));

        Attach attach = new Attach();
        attach.wrap(buffer, offset, true);

        assertEquals("topic.ABC-> (28ad69a4-6af6-4253-a5c9-7b456f294625)", attach.getName(READ_UTF_8));
        assertEquals(0L, attach.getHandle());
        assertEquals(Role.RECEIVER, attach.getRole());
        assertEquals(SenderSettleMode.UNSETTLED, attach.getSendSettleMode());
        assertEquals(ReceiverSettleMode.FIRST, attach.getReceiveSettleMode());

        Source source = attach.getSource();
        assertNotNull(source);

        //3 fields in the source
        assertEquals(3, source.getComposite().count());
        assertEquals("topic.ABC", source.getAddress(READ_UTF_8));
        assertEquals(TerminusDurability.NONE, source.getDurable());
        assertEquals(TerminusExpiryPolicy.LINK_DETACH, source.getExpiryPolicy());

        Target target = attach.getTarget();
        assertNotNull(target);

        //1 field in target
        assertEquals(1, target.getComposite().count());
        assertEquals("57f0ef16-fc7a-46f6-8bd4-d2eef55435a8", target.getAddress(READ_UTF_8));

    }

    @Theory
    public void shouldEncodeAndDecode(int offset)
    {
        Attach attach = new Attach();
        attach.wrap(buffer, offset, true)
                .maxLength(255)
                .setName(WRITE_UTF_8, "topic.ABC<-cd969dea-9998-4914-b9be-a6d25b476601")
                .setHandle(0L)
                .setRole(Role.SENDER)
                .setSendSettleMode(SenderSettleMode.UNSETTLED)
                .setReceiveSettleMode(ReceiverSettleMode.FIRST);

        attach.getSource()
                .setDescriptor()
                .maxLength(255)
                .setAddress(WRITE_UTF_8, "cd969dea-9998-4914-b9be-a6d25b476601")
                .setDurableNull()
                .setExpiryPolicyNull()
                .setTimeoutNull()
                .setDynamicNull()
                .setDynamicNodePropertiesNull()
                .setDistributionModeNull()
                .setFilterNull()
                .getDefaultOutcome().setDeliveryState(Outcome.ACCEPTED).getComposite().maxLength(0).clear();

        attach.getTarget()
                .setDescriptor()
                .maxLength(255)
                .setAddress(WRITE_UTF_8, "topic.ABC");

        assertEquals("topic.ABC<-cd969dea-9998-4914-b9be-a6d25b476601", attach.getName(READ_UTF_8));
        assertEquals(0L, attach.getHandle());
        assertEquals(Role.SENDER, attach.getRole());
        assertEquals(SenderSettleMode.UNSETTLED, attach.getSendSettleMode());
        assertEquals(ReceiverSettleMode.FIRST, attach.getReceiveSettleMode());

        Source source = attach.getSource();
        assertNotNull(source);

        //9 fields in the source
        assertEquals(9, source.getComposite().count());
        assertEquals("cd969dea-9998-4914-b9be-a6d25b476601", source.getAddress(READ_UTF_8));
        assertTrue(source.isDurableNull());
        assertTrue(source.isExpiryPolicyNull());
        assertTrue(source.isTimeoutNull());
        assertTrue(source.isDynamicNull());
        assertTrue(source.isDynamicNodePropertiesNull());
        assertTrue(source.isDistributionModeNull());
        assertTrue(source.isFilterNull());
        assertEquals(Outcome.ACCEPTED, source.getDefaultOutcome().getOutcome());

        Target target = attach.getTarget();
        assertNotNull(target);

        //1 field in target
        assertEquals(1, target.getComposite().count());
        assertEquals("topic.ABC", target.getAddress(READ_UTF_8));
    }

    @Theory
    public void shouldEncodeProducerResponse(int offset)
    {
        Attach attach = new Attach();

        attach.wrap(buffer, offset, true)
                .maxLength(255)
                .setName(WRITE_UTF_8, "topic.ABC<-cd969dea-9998-4914-b9be-a6d25b476601")
                .setHandle(0L)
                .setRole(Role.RECEIVER)
                .setSendSettleMode(SenderSettleMode.MIXED)
                .setReceiveSettleMode(ReceiverSettleMode.FIRST);

        attach.getSource()
                    .setDescriptor()
                    .maxLength(255)
                    .setAddress(WRITE_UTF_8, "cd969dea-9998-4914-b9be-a6d25b476601")
                    .setDurable(TerminusDurability.NONE)
                    .setExpiryPolicy(TerminusExpiryPolicy.SESSION_END)
                    .setTimeout(0L)
                    .setDynamic(false)
                    .setDynamicNodePropertiesNull()
                    .setDistributionModeNull()
                    .setFilterNull()
                    .getDefaultOutcome().setDeliveryState(Outcome.ACCEPTED).getComposite().maxLength(0).clear();
                    //.setOutcomes("amqp:accepted:list", "amqp:accepted:list") //TODO(JAF): Need to add support for this

        attach.getTarget()
                    .setDescriptor()
                    .maxLength(255)
                    .setAddress(WRITE_UTF_8, "topic.ABC");

        //Example from broker back to QPID JMS 0.28 Producer
        assertEquals("c08c07a12f746f7069632e4142433c2d63643936396465612d393939382d343931342d623962652d61366432356234373" +
                "6363031434150025000005328c03e09a12463643936396465612d393939382d343931342d623962652d61366432356234373636303" +
                "143a30b73657373696f6e2d656e64434240404000532445" +
                "005329c00c01a109746f7069632e414243", toHex(buffer.byteArray(), offset, 142));
    }

    @Theory
    public void shouldEncodeConsumerResponse(int offset)
    {
        Attach attach = new Attach();

        attach.wrap(buffer, offset, true)
                .maxLength(255)
                .setName(WRITE_UTF_8, "topic.ABC-> (28ad69a4-6af6-4253-a5c9-7b456f294625)")
                .setHandle(0L)
                .setRole(Role.SENDER)
                .setSendSettleMode(SenderSettleMode.MIXED)
                .setReceiveSettleMode(ReceiverSettleMode.FIRST);

        attach.getSource()
                .setDescriptor()
                .maxLength(255)
                .setAddress(WRITE_UTF_8, "topic.ABC")
                .setDurable(TerminusDurability.NONE)
                .setExpiryPolicy(TerminusExpiryPolicy.LINK_DETACH);

        attach.getTarget()
                .setDescriptor()
                .maxLength(255)
                .setAddress(WRITE_UTF_8, "57f0ef16-fc7a-46f6-8bd4-d2eef55435a8");

        attach.setUnsettledNull()
                .setIncompleteUnsettled(false)
                .setInitialDeliveryCount(0);


        //Example from broker back to QPID JMS 0.28 Consumer
        assertEquals("c0890aa132746f7069632e4142432d3e202832386164363961342d366166362d343235332d613563392d3762343536663" +
                "2393436323529434250025000005328c01a03a109746f7069632e41424343a30b6c696e6b2d646574616368005329c02701a12" +
                "435376630656631362d666337612d343666362d386264342d643265656635353433356138404243",
                toHex(buffer.byteArray(), offset, 139));
    }
}

