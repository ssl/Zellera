package zel.protocol.outgoing.rooms.users

import zel.protocol.generated.IgnoreResultOut
import zel.protocol.generated.IgnoreResultOutCodec
import zel.protocol.outgoing.OutgoingMessage

class IgnoreResult(
    private val userId: Int,
) : OutgoingMessage<IgnoreResultOut>(IgnoreResultOutCodec) {
    override fun message() = IgnoreResultOut(IGNORED, userId)

    private companion object {
        const val IGNORED = 1
    }
}
