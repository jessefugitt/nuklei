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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.kaazing.nuklei.amqp_1_0.codec.util.FieldAccessors.newAccessor;
import static org.kaazing.nuklei.amqp_1_0.codec.util.FieldMutators.newMutator;
import static uk.co.real_logic.agrona.BitUtil.fromHex;
import static uk.co.real_logic.agrona.BitUtil.toHex;

@RunWith(Theories.class)
public class FlowTest
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
    public void shouldEncodeBeginResponse(int offset)
    {
        Flow flow = new Flow();
        flow.wrap(buffer, offset, true)
                .maxLength(255)
                .setNextIncomingId(0)
                .setIncomingWindow(Integer.MAX_VALUE)
                .setNextOutgoingId(1)
                .setOutgoingWindow(0);

        assertEquals(offset + 12, flow.limit());
        assertEquals("c00a0443707fffffff520143", toHex(buffer.byteArray(), offset, 12));
    }

    @Theory
    public void shouldEncodeProducerResponse(int offset)
    {
        Flow flow = new Flow();
        flow.wrap(buffer, offset, true)
                .maxLength(255)
                .setNextIncomingId(0)
                .setIncomingWindow(Integer.MAX_VALUE)
                .setNextOutgoingId(1)
                .setOutgoingWindow(0)
                .setHandle(0)
                .setDeliveryCount(0)
                .setLinkCredit(1000);

        assertEquals(offset + 19, flow.limit());
        assertEquals("c0110743707fffffff520143434370000003e8", toHex(buffer.byteArray(), offset, 19));
    }

    @Theory
    public void shouldDecodeFromConsumer(int offset)
    {
        buffer.putBytes(offset, fromHex("c0150a5201700000080043700000080043435264404242"));

        Flow flow = new Flow();
        flow.wrap(buffer, offset, true);

        assertEquals(21, flow.length());

        assertEquals(1L, flow.getNextIncomingId());
        assertEquals(2048L, flow.getIncomingWindow());
        assertEquals(0L, flow.getNextOutgoingId());
        assertEquals(2048L, flow.getOutgoingWindow());
        assertEquals(0L, flow.getHandle());
        assertEquals(0L, flow.getDeliveryCount());
        assertEquals(100L, flow.getLinkCredit());
        assertTrue(flow.isAvailableNull());
        assertFalse(flow.getDrain());
        assertFalse(flow.getEcho());
    }

    @Theory
    public void shouldEncodeAndDecode(int offset)
    {
        Flow flow = new Flow();
        flow.wrap(buffer, offset, true)
                .maxLength(255)
                .setNextIncomingId(0)
                .setIncomingWindow(Integer.MAX_VALUE)
                .setNextOutgoingId(1)
                .setOutgoingWindow(0)
                .setHandle(0)
                .setDeliveryCount(0)
                .setLinkCredit(1000);

        assertEquals(17, flow.length());

        assertEquals(0L, flow.getNextIncomingId());
        assertEquals(Integer.MAX_VALUE, flow.getIncomingWindow());
        assertEquals(1L, flow.getNextOutgoingId());
        assertEquals(0L, flow.getOutgoingWindow());
        assertEquals(0L, flow.getHandle());
        assertEquals(0L, flow.getDeliveryCount());
        assertEquals(1000L, flow.getLinkCredit());
    }
}

