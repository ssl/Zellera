package zel.protocol.generated

data class PollRejectIn(
    val pollId: Int,
)

object PollRejectInCodec : IncomingCodec<PollRejectIn> {
    override val name = "PollRejectIn"

    override fun read(r: PacketReader): PollRejectIn = readPollRejectIn(r)

    override fun write(w: PacketWriter, m: PollRejectIn) = writePollRejectIn(w, m)

    private fun readPollRejectIn(r: PacketReader): PollRejectIn {
        val vPollId = r.int()
        return PollRejectIn(vPollId)
    }

    private fun writePollRejectIn(w: PacketWriter, m: PollRejectIn) {
        w.int(m.pollId)
    }
}
