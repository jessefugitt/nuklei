package org.kaazing.nuklei.amqp_1_0.aeron;

import uk.co.real_logic.aeron.common.concurrent.logbuffer.Header;
import uk.co.real_logic.agrona.DirectBuffer;

/**
 *
 */
public class AeronMessage implements Message
{
    private DirectBuffer buffer;
    private int offset;
    private int length;
    private Header header;

    public AeronMessage(DirectBuffer buffer, int offset, int length, Header header)
    {
        this.buffer = buffer;
        this.offset = offset;
        this.length = length;
        this.header = header;
    }

    @Override
    public DirectBuffer getBuffer()
    {
        return buffer;
    }

    @Override
    public int getOffset()
    {
        return offset;
    }

    @Override
    public int getLength()
    {
        return length;
    }
}
