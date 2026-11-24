package zel.protocol.generated

data class PresentOpenIn(
    val int1: Int,
)

object PresentOpenInCodec : IncomingCodec<PresentOpenIn> {
    override val name = "PresentOpenIn"

    override fun read(r: PacketReader): PresentOpenIn = readPresentOpenIn(r)

    override fun write(w: PacketWriter, m: PresentOpenIn) = writePresentOpenIn(w, m)

    private fun readPresentOpenIn(r: PacketReader): PresentOpenIn {
        val vInt1 = r.int()
        return PresentOpenIn(vInt1)
    }

    private fun writePresentOpenIn(w: PacketWriter, m: PresentOpenIn) {
        w.int(m.int1)
    }
}
