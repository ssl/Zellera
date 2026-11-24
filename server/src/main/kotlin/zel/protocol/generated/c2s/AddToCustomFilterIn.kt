package zel.protocol.generated

data class AddToCustomFilterIn(
    val word: String,
)

object AddToCustomFilterInCodec : IncomingCodec<AddToCustomFilterIn> {
    override val name = "AddToCustomFilterIn"

    override fun read(r: PacketReader): AddToCustomFilterIn = readAddToCustomFilterIn(r)

    override fun write(w: PacketWriter, m: AddToCustomFilterIn) = writeAddToCustomFilterIn(w, m)

    private fun readAddToCustomFilterIn(r: PacketReader): AddToCustomFilterIn {
        val vWord = r.string()
        return AddToCustomFilterIn(vWord)
    }

    private fun writeAddToCustomFilterIn(w: PacketWriter, m: AddToCustomFilterIn) {
        w.string(m.word)
    }
}
