package zel.protocol.outgoing.users

import zel.protocol.generated.ModifyCustomFilterResultOut
import zel.protocol.generated.ModifyCustomFilterResultOutCodec
import zel.protocol.outgoing.OutgoingMessage

class CustomWordFilterModifyResult(
    private val result: Int,
    word: String?,
) : OutgoingMessage<ModifyCustomFilterResultOut>(ModifyCustomFilterResultOutCodec) {
    private val word = word.orEmpty()

    override fun message() = ModifyCustomFilterResultOut(result, word)

    companion object {
        const val FAILED = 0
        const val ADDED = 1
        const val REMOVED = 3
    }
}
