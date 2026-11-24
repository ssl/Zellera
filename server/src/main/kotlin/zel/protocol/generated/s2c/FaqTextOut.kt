package zel.protocol.generated

data class FaqTextOut(
    val int1: Int,
    val string1: String,
)

object FaqTextOutCodec : OutgoingCodec<FaqTextOut> {
    override val name = "FaqTextOut"

    override fun read(r: PacketReader): FaqTextOut = readFaqTextOut(r)

    override fun write(w: PacketWriter, m: FaqTextOut) = writeFaqTextOut(w, m)

    private fun readFaqTextOut(r: PacketReader): FaqTextOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        return FaqTextOut(vInt1, vString1)
    }

    private fun writeFaqTextOut(w: PacketWriter, m: FaqTextOut) {
        w.int(m.int1)
        w.string(m.string1)
    }
}
