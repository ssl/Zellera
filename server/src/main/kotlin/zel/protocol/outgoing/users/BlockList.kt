package zel.protocol.outgoing.users

import zel.protocol.generated.BlockListOut
import zel.protocol.generated.BlockListOutCodec
import zel.protocol.outgoing.OutgoingMessage

class BlockList(
    blockedUserIds: List<Int>,
) : OutgoingMessage<BlockListOut>(BlockListOutCodec) {
    private val blockedUserIds = blockedUserIds.toList()

    override fun message() = BlockListOut(blockedUserIds)
}
