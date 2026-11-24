package zel.protocol.generated

data class FigureSetIdRemovedOut(
    val int1: Int,
)

object FigureSetIdRemovedOutCodec : OutgoingCodec<FigureSetIdRemovedOut> {
    override val name = "FigureSetIdRemovedOut"

    override fun read(r: PacketReader): FigureSetIdRemovedOut = readFigureSetIdRemovedOut(r)

    override fun write(w: PacketWriter, m: FigureSetIdRemovedOut) = writeFigureSetIdRemovedOut(w, m)

    private fun readFigureSetIdRemovedOut(r: PacketReader): FigureSetIdRemovedOut {
        val vInt1 = r.int()
        return FigureSetIdRemovedOut(vInt1)
    }

    private fun writeFigureSetIdRemovedOut(w: PacketWriter, m: FigureSetIdRemovedOut) {
        w.int(m.int1)
    }
}
