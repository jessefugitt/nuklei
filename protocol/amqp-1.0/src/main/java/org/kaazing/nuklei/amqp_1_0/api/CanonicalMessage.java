package org.kaazing.nuklei.amqp_1_0.api;

import uk.co.real_logic.agrona.DirectBuffer;

/**
 *
 *
 */
public interface CanonicalMessage
{
    DirectBuffer getBuffer();
    int getOffset();
    int getLength();
    //TODO(JAF): Later there might be a getProperties or some way to get at the header information
}
