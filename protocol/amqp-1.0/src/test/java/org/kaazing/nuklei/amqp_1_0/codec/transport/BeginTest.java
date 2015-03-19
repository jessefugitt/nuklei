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
import org.kaazing.nuklei.function.DirectBufferAccessor;
import org.kaazing.nuklei.function.MutableDirectBufferMutator;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.util.Random;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.kaazing.nuklei.amqp_1_0.codec.util.FieldAccessors.newAccessor;
import static org.kaazing.nuklei.amqp_1_0.codec.util.FieldMutators.newMutator;
import static uk.co.real_logic.agrona.BitUtil.fromHex;
import static uk.co.real_logic.agrona.BitUtil.toHex;

@RunWith(Theories.class)
public class BeginTest
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
        Begin begin = new Begin();

        begin.wrap(buffer, offset, true)
                .maxLength(255)
                .setRemoteChannel(0)
                .setNextOutgoingId(1)
                .setIncomingWindow(0)
                .setOutgoingWindow(0)
                .setHandleMax(1024);

        assertEquals(offset + 15, begin.limit());
        assertEquals("c00d05600000520143437000000400", toHex(buffer.byteArray(), offset, 15));
    }

    @Theory
    public void shouldDecode(int offset)
    {
        //Example from QPID JMS 0.28 Producer connecting to a broker
        buffer.putBytes(offset, fromHex("c0120540437000000800700000080070ffffffff"));

        Begin begin = new Begin();
        begin.wrap(buffer, offset, true);

        assertEquals(18, begin.length());

        //TODO: No way to check this value for null
        //assertNull(begin.getRemoteChannel());
        //assertEquals(Type.Kind.NULL, begin.getRemoteChannelType().kind());

        assertEquals(0L, begin.getNextOutgoingId());
        assertEquals(2048L, begin.getIncomingWindow());
        assertEquals(2048L, begin.getOutgoingWindow());
        assertEquals(4294967295L, begin.getHandleMax());
    }

    @Theory
    public void shouldEncodeAndDecode(int offset)
    {
        Begin begin = new Begin();

        begin.wrap(buffer, offset, true)
                .maxLength(255)
                .setRemoteChannel(0)
                .setNextOutgoingId(1)
                .setIncomingWindow(0)
                .setOutgoingWindow(0)
                .setHandleMax(1024);

        assertEquals(13, begin.length());

        //TODO: No way to check this value for null
        //assertNull(begin.getRemoteChannel());
        assertEquals(1L, begin.getNextOutgoingId());
        assertEquals(0L, begin.getIncomingWindow());
        assertEquals(0L, begin.getOutgoingWindow());
        assertEquals(1024L, begin.getHandleMax());
    }
}

