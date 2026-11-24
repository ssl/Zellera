package zel.protocol.generated

data class FigureUpdateOut(
    val string1: String,
    val string2: String,
)

object FigureUpdateOutCodec : OutgoingCodec<FigureUpdateOut> {
    override val name = "FigureUpdateOut"

    override fun read(r: PacketReader): FigureUpdateOut = readFigureUpdateOut(r)

    override fun write(w: PacketWriter, m: FigureUpdateOut) = writeFigureUpdateOut(w, m)

    private fun readFigureUpdateOut(r: PacketReader): FigureUpdateOut {
        val vString1 = r.string()
        val vString2 = r.string()
        return FigureUpdateOut(vString1, vString2)
    }

    private fun writeFigureUpdateOut(w: PacketWriter, m: FigureUpdateOut) {
        w.string(m.string1)
        w.string(m.string2)
    }
}
