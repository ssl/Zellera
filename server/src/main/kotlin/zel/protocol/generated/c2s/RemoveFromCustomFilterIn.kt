package zel.protocol.generated

data class RemoveFromCustomFilterIn(
    val word: String,
)

object RemoveFromCustomFilterInCodec : IncomingCodec<RemoveFromCustomFilterIn> {
    override val name = "RemoveFromCustomFilterIn"

    override fun read(r: PacketReader): RemoveFromCustomFilterIn = readRemoveFromCustomFilterIn(r)

    override fun write(w: PacketWriter, m: RemoveFromCustomFilterIn) = writeRemoveFromCustomFilterIn(w, m)

    private fun readRemoveFromCustomFilterIn(r: PacketReader): RemoveFromCustomFilterIn {
        val vWord = r.string()
        return RemoveFromCustomFilterIn(vWord)
    }

    private fun writeRemoveFromCustomFilterIn(w: PacketWriter, m: RemoveFromCustomFilterIn) {
        w.string(m.word)
    }
}
