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
import org.kaazing.nuklei.amqp_1_0.codec.messaging.Message;
import org.kaazing.nuklei.function.DirectBufferAccessor;
import org.kaazing.nuklei.function.MutableDirectBufferMutator;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.util.Random;

import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.kaazing.nuklei.amqp_1_0.codec.messaging.Performative.TRANSFER;
import static org.kaazing.nuklei.amqp_1_0.codec.util.FieldAccessors.newAccessor;
import static org.kaazing.nuklei.amqp_1_0.codec.util.FieldMutators.newMutator;
import static uk.co.real_logic.agrona.BitUtil.fromHex;
import static uk.co.real_logic.agrona.BitUtil.toHex;

@RunWith(Theories.class)
public class TransferTest
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
    public void shouldEncodePayloadOnly(int offset)
    {
        Transfer transfer = new Transfer();

        transfer.wrap(buffer, offset, true)
                .maxLength(255)
                .setHandle(0L)
                .setDeliveryId(1L)
                .setDeliveryTag(WRITE_UTF_8, "\0")
                .setMessageFormat(0)
                .setSettled(true);
        transfer.getMessage()
                .setPayloadOnly(true)
                .setDescriptor(0x77L)
                .setValue(WRITE_UTF_8, new String("Message 0 at Thu Apr 02 10:24:55 CDT 2015"));


        assertEquals(offset + 57, transfer.limit());
        assertEquals("c00905435201a001004341" +
                "005377a1294d657373616765203020617420546875204170722030322031303a32343a3535204344542032303135",
                toHex(buffer.byteArray(), offset, 57));
    }

    @Theory
    public void shouldDecodePayloadOnly(int offset)
    {
        buffer.putBytes(offset, fromHex("c00905435201a001004341" +
                "005377a1294d657373616765203020617420546875204170722030322031303a32343a3535204344542032303135"));

        Transfer transfer = new Transfer();
        transfer.wrap(buffer, offset, true);

        assertEquals(9, transfer.length());

        assertEquals(0L, transfer.getHandle());
        assertEquals(1L, transfer.getDeliveryId());
        assertEquals("\0", transfer.getDeliveryTag(READ_UTF_8));
        assertEquals(0L, transfer.getMessageFormat());
        assertEquals(true, transfer.getSettled());

        Message message = transfer.getMessage();
        assertNotNull(message);
        //TODO(JAF): This is an implementation workaround to support payload only messages at the moment
        message.setPayloadOnly(true);

        // get the string out
        String messageString = message.getValue(READ_UTF_8);
        assertEquals("Message 0 at Thu Apr 02 10:24:55 CDT 2015", messageString);
    }

    @Theory
    public void shouldEncodeAndDecode(int offset)
    {
        Transfer transfer = new Transfer();

        transfer.wrap(buffer, offset, true)
                .maxLength(255)
                .setHandle(0L)
                .setDeliveryId(1L)
                .setDeliveryTag(WRITE_UTF_8, "\0")
                .setMessageFormat(0)
                .setSettled(true);
        transfer.getMessage()
                .setPayloadOnly(true)
                .setDescriptor(0x77L)
                .setValue(WRITE_UTF_8, new String("Message 0 at Thu Apr 02 10:24:55 CDT 2015"));

        assertEquals(9, transfer.length());

        assertEquals(0L, transfer.getHandle());
        assertEquals(1L, transfer.getDeliveryId());
        assertEquals("\0", transfer.getDeliveryTag(READ_UTF_8));
        assertEquals(0L, transfer.getMessageFormat());
        assertEquals(true, transfer.getSettled());

        Message message = transfer.getMessage();
        assertNotNull(message);
        //TODO(JAF): This is an implementation workaround to support payload only messages at the moment
        message.setPayloadOnly(true);

        // get the string out
        String messageString = message.getValue(READ_UTF_8);
        assertEquals("Message 0 at Thu Apr 02 10:24:55 CDT 2015", messageString);
    }


    @Theory
    public void shouldDecodeTransferWithFullyFormattedMessage(int offset)
    {
        //Example from QPID JMS 0.28 Producer connecting to a broker
        buffer.putBytes(offset, fromHex("c008054343a001304342" + //transfer performative
                "005370c00402415004" + //message header
                "005372c11702a30d782d6f70742d746f2d74797065a1057175657565" + //message annotations
                "005373c0480aa02431376538396536622d663761642d343263622d393961392d36393066393933663434393140a10f717565756" +
                        "53a2f2f71756575652d41404040404040830000014c7aba3d6d" + //message properties
                "005374c10100" + //application properties
                "005377a1294d657373616765203020617420546875204170722030322031303a32323a3437204344542032303135" + //payload
                "005378c10100" //footer
        ));

        Transfer transfer = new Transfer();
        transfer.wrap(buffer, offset, true);

        assertEquals(8, transfer.length());

        assertEquals(0L, transfer.getHandle());
        assertEquals(0L, transfer.getDeliveryId());
        assertEquals("0", transfer.getDeliveryTag(READ_UTF_8));
        assertEquals(0L, transfer.getMessageFormat());
        assertEquals(false, transfer.getSettled());

        Message message = transfer.getMessage();
        assertNotNull(message);
        assertFalse(message.isPayloadOnly());

        // get the string out
        String messageString = message.getValue(READ_UTF_8);
        assertEquals("Message 0 at Thu Apr 02 10:22:47 CDT 2015", messageString);
    }
}

