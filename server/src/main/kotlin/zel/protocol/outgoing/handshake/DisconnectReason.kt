package zel.protocol.outgoing.handshake

import zel.protocol.generated.DisconnectReasonOut
import zel.protocol.generated.DisconnectReasonOutCodec
import zel.protocol.outgoing.OutgoingMessage

class DisconnectReason(
    private val reason: Int,
) : OutgoingMessage<DisconnectReasonOut>(DisconnectReasonOutCodec) {
    override fun message() = DisconnectReasonOut(reason)

    companion object {
        const val JUST_BANNED = 1
        const val CONCURRENT_LOGIN = 2
        const val PONG_TIMEOUT = 113
    }
}
