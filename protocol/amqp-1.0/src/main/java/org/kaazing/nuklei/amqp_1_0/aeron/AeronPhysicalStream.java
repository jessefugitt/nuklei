package org.kaazing.nuklei.amqp_1_0.aeron;

/**
 *
 *
 */
public class AeronPhysicalStream
{
    private final String channel; //ex: udp://localhost:40123
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

    //TODO(JAF): Change to URI format instead of using comma to concatenate

    public static AeronPhysicalStream fromString(String channelStream)
    {
        String[] tokens = channelStream.split(",");
        String parsedChannel = tokens[0];
        int parsedStreamId = tokens.length > 1 ? Integer.parseInt(tokens[1]) : 0;
        return new AeronPhysicalStream(parsedChannel, parsedStreamId);
    }
}
