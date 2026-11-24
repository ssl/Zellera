package zel.protocol.generated

data class PollStartIn(
    val pollId: Int,
)

object PollStartInCodec : IncomingCodec<PollStartIn> {
    override val name = "PollStartIn"

    override fun read(r: PacketReader): PollStartIn = readPollStartIn(r)

    override fun write(w: PacketWriter, m: PollStartIn) = writePollStartIn(w, m)

    private fun readPollStartIn(r: PacketReader): PollStartIn {
        val vPollId = r.int()
        return PollStartIn(vPollId)
    }

    private fun writePollStartIn(w: PacketWriter, m: PollStartIn) {
        w.int(m.pollId)
    }
}
