package zel.protocol.outgoing.users

import zel.protocol.generated.BanInfoOut
import zel.protocol.generated.BanInfoOutCodec
import zel.protocol.outgoing.OutgoingMessage

class BanInfo(
    target: Int,
    reason: String?,
    banExpirySeconds: Int,
    localizedReason: String?,
) : OutgoingMessage<BanInfoOut>(BanInfoOutCodec) {
    private val message =
        BanInfoOut(target.toShort(), reason.orEmpty(), banExpirySeconds, localizedReason.orEmpty())

    override fun message() = message
}
