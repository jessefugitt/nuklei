package org.kaazing.nuklei.amqp_1_0.aeron;

/**
 *
 *
 */
public class AeronPhysicalStream
{
    private final String channel; //ex: udp://localhost:40123
    private final int stream; // ex: 10

    public AeronPhysicalStream(String channel, int stream)
    {
        this.channel = channel;
        this.stream = stream;
    }

    public String getChannel()
    {
        return channel;
    }

    public int getStream()
    {
        return stream;
    }

    //TODO(JAF): Change to URI format instead of using comma to concatenate

    public String toString(String channel, int stream)
    {
        return channel + "," + stream;
    }

    public static AeronPhysicalStream fromString(String channelStream)
    {
        String[] tokens = channelStream.split(",");
        String parsedChannel = tokens[0];
        int parsedStream = tokens.length > 1 ? Integer.parseInt(tokens[1]) : 0;
        return new AeronPhysicalStream(parsedChannel, parsedStream);
    }
}
