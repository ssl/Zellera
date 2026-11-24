package zel.protocol.generated

data object PollErrorOut

object PollErrorOutCodec : OutgoingCodec<PollErrorOut> {
    override val name = "PollErrorOut"

    override fun read(r: PacketReader): PollErrorOut = PollErrorOut

    override fun write(w: PacketWriter, m: PollErrorOut) = writePollErrorOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writePollErrorOut(w: PacketWriter, m: PollErrorOut) {
    }
}
