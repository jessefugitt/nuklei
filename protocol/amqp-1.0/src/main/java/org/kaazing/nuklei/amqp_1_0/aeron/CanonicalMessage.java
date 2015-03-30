package org.kaazing.nuklei.amqp_1_0.aeron;

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
}
