package zel.protocol.outgoing.users

import zel.protocol.generated.BlockPlayerUpdateOut
import zel.protocol.generated.BlockPlayerUpdateOutCodec
import zel.protocol.outgoing.OutgoingMessage

class BlockResult(
    private val result: Int,
    private val userId: Int,
) : OutgoingMessage<BlockPlayerUpdateOut>(BlockPlayerUpdateOutCodec) {
    override fun message() = BlockPlayerUpdateOut(result, userId)

    companion object {
        const val UNBLOCKED = 0
        const val BLOCKED = 1
    }
}
