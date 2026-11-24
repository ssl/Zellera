package zel.protocol.outgoing.users

import zel.protocol.generated.PlayerNftChatStylesOut
import zel.protocol.generated.PlayerNftChatStylesOutCodec
import zel.protocol.outgoing.OutgoingMessage

class NftChatStyles(
    styleIds: Collection<Int>?,
) : OutgoingMessage<PlayerNftChatStylesOut>(PlayerNftChatStylesOutCodec) {
    val styleIds: List<Int> = styleIds?.toList().orEmpty()

    override fun message() = PlayerNftChatStylesOut(styleIds)
}
