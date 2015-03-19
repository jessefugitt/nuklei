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

import org.junit.Ignore;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.kaazing.nuklei.amqp_1_0.codec.definitions.ReceiverSettleMode;
import org.kaazing.nuklei.amqp_1_0.codec.definitions.Role;
import org.kaazing.nuklei.amqp_1_0.codec.definitions.SenderSettleMode;
import org.kaazing.nuklei.amqp_1_0.codec.messaging.Source;
import org.kaazing.nuklei.amqp_1_0.codec.messaging.TerminusDurability;
import org.kaazing.nuklei.amqp_1_0.codec.messaging.TerminusExpiryPolicy;
import org.kaazing.nuklei.amqp_1_0.codec.types.*;
import org.kaazing.nuklei.function.DirectBufferAccessor;
import org.kaazing.nuklei.function.MutableDirectBufferMutator;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.util.Random;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
    public void shouldDecode(int offset)
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
        assertEquals("cd969dea-9998-4914-b9be-a6d25b476601", source.getAddress(READ_UTF_8));

        //TODO: No way to check this value for null and source.getDurable throws an access error (IllegalStateException)
        //assertNull(source.getDurable);
        TerminusDurability terminusDurability = source.getDurable();

        //Can't parse anything past the source

    }

    @Theory
    @Ignore
    public void shouldEncode(int offset)
    {
        Attach attach = new Attach();

        attach.wrap(buffer, offset, true)
                .maxLength(255)
                .setName(WRITE_UTF_8, "topic.ABC<-cd969dea-9998-4914-b9be-a6d25b47660")
                .setHandle(0L)
                .setRole(Role.RECEIVER)
                .setSendSettleMode(SenderSettleMode.MIXED)
                .setReceiveSettleMode(ReceiverSettleMode.FIRST);

        //Can't encode a source
        attach.getSource()
                    .maxLength(255)
                    .setAddress(WRITE_UTF_8, "cd969dea-9998-4914-b9be-a6d25b47660")
                    .setDurable(TerminusDurability.NONE)
                    .setExpiryPolicy(TerminusExpiryPolicy.SESSION_END)
                    .setTimeout(0L)
                    .setDynamic(false);
                    //.getDynamicNodeProperties() //This should be null
                    //.setDistributionMode() //This should be null
                    //.getFilter() //This should be null
                    //.setDefaultOutcome(ACCEPTED) //Should there be a setter here
                    //.setOutcomes("amqp:accepted:list", "amqp:accepted:list")

        attach.getTarget()
                    .maxLength(255)
                    .setAddress(WRITE_UTF_8, "topic.ABC");

        //assertEquals(offset + N, attach.limit());
        assertEquals("c0b607a12f746f7069632e4142433c2d63643936396465612d393939382d343931342d623962652d61366432356234373" +
            "6363031434150025000005328c0680aa12463643936396465612d393939382d343931342d623962652d61366432356234373636303" +
            "143a30b73657373696f6e2d656e64434240404000532445e02802a312616d71703a61636365707465643a6c69737412616d71703a7" +
            "2656a65637465643a6c697374005329c00c01a109746f7069632e414243", toHex(buffer.byteArray(), offset, 15));
            //Update 15 to correct value
    }

    @Theory
    public void shouldEncodeAndDecode(int offset)
    {
        Attach attach = new Attach();
        //TODO: Implement both encode and decode
    }

}

