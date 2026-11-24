package zel.protocol.outgoing.users

import zel.protocol.generated.CheckPlayerNameResultOut
import zel.protocol.generated.CheckPlayerNameResultOutCodec
import zel.protocol.outgoing.OutgoingMessage

class ChangeNameCheckResult(
    val verdict: NameVerdict,
    val name: String,
    val alternatives: List<String> = emptyList(),
) : OutgoingMessage<CheckPlayerNameResultOut>(CheckPlayerNameResultOutCodec) {
    @Suppress("MagicNumber") // the verdict numbers the client reads
    enum class NameVerdict(
        val code: Int,
    ) {
        FREE(0),
        SHORT(2),
        LONG(3),
        UNFIT(4),
        TAKEN(5),
    }

    override fun message() = CheckPlayerNameResultOut(verdict.code, name, alternatives)
}
