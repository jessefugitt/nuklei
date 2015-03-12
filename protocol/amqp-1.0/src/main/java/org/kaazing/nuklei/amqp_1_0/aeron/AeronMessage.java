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

    @Override
    public DirectBuffer getBuffer()
    {
        return buffer;
    }

    public void setBuffer(DirectBuffer buffer)
    {
        this.buffer = buffer;
    }

    @Override
    public int getOffset()
    {
        return offset;
    }

    public void setOffset(int offset)
    {
        this.offset = offset;
    }

    @Override
    public int getLength()
    {
        return length;
    }

    public void setLength(int length)
    {
        this.length = length;
    }

    public Header getHeader()
    {
        return header;
    }

    public void setHeader(Header header)
    {
        this.header = header;
    }
}
