package zel.protocol.outgoing.users

import zel.protocol.generated.RemainingMutePeriodOut
import zel.protocol.generated.RemainingMutePeriodOutCodec
import zel.protocol.outgoing.OutgoingMessage

class RemainingMutePeriod(
    seconds: Int,
) : OutgoingMessage<RemainingMutePeriodOut>(RemainingMutePeriodOutCodec) {
    val seconds: Int = maxOf(0, seconds)

    override fun message() = RemainingMutePeriodOut(seconds)
}
