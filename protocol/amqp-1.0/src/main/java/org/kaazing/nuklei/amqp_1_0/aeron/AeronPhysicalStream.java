package org.kaazing.nuklei.amqp_1_0.aeron;

import uk.co.real_logic.aeron.common.uri.AeronUri;

/**
 *
 *
 */
public class AeronPhysicalStream
{
    private final String channel; //ex: aeron:udp?remote=127.0.0.1:30123
    private final int streamId; // ex: 10

    public AeronPhysicalStream(String channel, int streamId)
    {
        this.channel = channel;
        this.streamId = streamId;
    }

    public String getChannel()
    {
        return channel;
    }

    public int getStreamId()
    {
        return streamId;
    }

    public static AeronPhysicalStream fromString(String channelStream)
    {
        //channelStream looks like: aeron:udp?remote=127.0.0.1:30123|streamId=10
        AeronUri aeronUri = AeronUri.parse(channelStream);
        int parsedStreamId = 0;
        if(aeronUri.get("streamId") != null)
        {
            parsedStreamId = Integer.parseInt(aeronUri.get("streamId"));
        }
        return new AeronPhysicalStream(channelStream, parsedStreamId);
    }
}
