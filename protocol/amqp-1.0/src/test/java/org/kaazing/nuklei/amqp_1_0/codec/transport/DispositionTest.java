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
import org.kaazing.nuklei.amqp_1_0.codec.definitions.Role;
import org.kaazing.nuklei.amqp_1_0.codec.messaging.DeliveryState;
import org.kaazing.nuklei.function.DirectBufferAccessor;
import org.kaazing.nuklei.function.MutableDirectBufferMutator;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.util.Random;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.kaazing.nuklei.amqp_1_0.codec.definitions.Role.RECEIVER;
import static org.kaazing.nuklei.amqp_1_0.codec.util.FieldAccessors.newAccessor;
import static org.kaazing.nuklei.amqp_1_0.codec.util.FieldMutators.newMutator;
import static uk.co.real_logic.agrona.BitUtil.fromHex;
import static uk.co.real_logic.agrona.BitUtil.toHex;

@RunWith(Theories.class)
public class DispositionTest
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
    public void shouldEncode(int offset)
    {
        Disposition disposition = new Disposition();

        disposition.wrap(buffer, offset, true)
                .maxLength(255)
                .setRole(RECEIVER)
                .setFirst(0L)
                .setLast(0L)
                .setSettled(true)
                .getState().setDeliveryState(DeliveryState.ACCEPTED);

        assertEquals(offset + 11, disposition.limit());
        assertEquals("c009054143434100532445", toHex(buffer.byteArray(), offset, 11));
    }

    @Theory
    public void shouldDecode(int offset)
    {
        buffer.putBytes(offset, fromHex("c009054143434100532445"));

        Disposition disposition = new Disposition();
        disposition.wrap(buffer, offset, true);

        assertEquals(9, disposition.length());

        assertEquals(Role.RECEIVER, disposition.getRole());
        assertEquals(0L, disposition.getFirst());
        assertEquals(0L, disposition.getLast());
        assertEquals(true, disposition.getSettled());
        assertEquals(DeliveryState.ACCEPTED, disposition.getState().getDeliveryState());
    }

    @Theory
    public void shouldEncodeAndDecode(int offset)
    {
        Disposition disposition = new Disposition();

        disposition.wrap(buffer, offset, true)
                .maxLength(255)
                .setRole(RECEIVER)
                .setFirst(0L)
                .setLast(0L)
                .setSettled(true)
                .getState().setDeliveryState(DeliveryState.ACCEPTED);

        assertEquals(9, disposition.length());

        assertEquals(Role.RECEIVER, disposition.getRole());
        assertEquals(0L, disposition.getFirst());
        assertEquals(0L, disposition.getLast());
        assertEquals(true, disposition.getSettled());
        assertEquals(DeliveryState.ACCEPTED, disposition.getState().getDeliveryState());
    }
}

