package zel.protocol.generated

data class SetMannequinFigureIn(
    val itemId: Int,
)

object SetMannequinFigureInCodec : IncomingCodec<SetMannequinFigureIn> {
    override val name = "SetMannequinFigureIn"

    override fun read(r: PacketReader): SetMannequinFigureIn = readSetMannequinFigureIn(r)

    override fun write(w: PacketWriter, m: SetMannequinFigureIn) = writeSetMannequinFigureIn(w, m)

    private fun readSetMannequinFigureIn(r: PacketReader): SetMannequinFigureIn {
        val vItemId = r.int()
        return SetMannequinFigureIn(vItemId)
    }

    private fun writeSetMannequinFigureIn(w: PacketWriter, m: SetMannequinFigureIn) {
        w.int(m.itemId)
    }
}
