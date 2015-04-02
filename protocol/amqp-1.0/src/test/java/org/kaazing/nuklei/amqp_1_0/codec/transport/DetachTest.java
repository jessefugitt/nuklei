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
public class DetachTest
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
        Detach detach = new Detach();

        detach.wrap(buffer, offset, true)
                .maxLength(255)
                .setHandle(0)
                .setClosed(true);

        assertEquals(offset + 5, detach.limit());
        assertEquals("c003024341", toHex(buffer.byteArray(), offset, 5));
    }

    @Theory
    public void shouldDecode(int offset)
    {
        buffer.putBytes(offset, fromHex("c003024341"));

        Detach detach = new Detach();
        detach.wrap(buffer, offset, true);

        assertEquals(3, detach.length());

        assertEquals(0L, detach.getHandle());
        assertEquals(true, detach.getClosed());

    }

    @Theory
    public void shouldEncodeAndDecode(int offset)
    {
        Detach detach = new Detach();

        detach.wrap(buffer, offset, true)
                .maxLength(255)
                .setHandle(0)
                .setClosed(true);

        assertEquals(3, detach.length());

        assertEquals(0L, detach.getHandle());
        assertEquals(true, detach.getClosed());
    }
}

