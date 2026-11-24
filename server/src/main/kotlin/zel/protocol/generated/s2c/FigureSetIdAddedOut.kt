package zel.protocol.generated

data class FigureSetIdAddedOut(
    val int1: Int,
)

object FigureSetIdAddedOutCodec : OutgoingCodec<FigureSetIdAddedOut> {
    override val name = "FigureSetIdAddedOut"

    override fun read(r: PacketReader): FigureSetIdAddedOut = readFigureSetIdAddedOut(r)

    override fun write(w: PacketWriter, m: FigureSetIdAddedOut) = writeFigureSetIdAddedOut(w, m)

    private fun readFigureSetIdAddedOut(r: PacketReader): FigureSetIdAddedOut {
        val vInt1 = r.int()
        return FigureSetIdAddedOut(vInt1)
    }

    private fun writeFigureSetIdAddedOut(w: PacketWriter, m: FigureSetIdAddedOut) {
        w.int(m.int1)
    }
}
