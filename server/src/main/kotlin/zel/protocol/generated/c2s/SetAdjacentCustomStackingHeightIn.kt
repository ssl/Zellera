package zel.protocol.generated

data class SetAdjacentCustomStackingHeightIn(
    val itemId: Int,
    val moveDown: Boolean,
)

object SetAdjacentCustomStackingHeightInCodec : IncomingCodec<SetAdjacentCustomStackingHeightIn> {
    override val name = "SetAdjacentCustomStackingHeightIn"

    override fun read(r: PacketReader): SetAdjacentCustomStackingHeightIn = readSetAdjacentCustomStackingHeightIn(r)

    override fun write(w: PacketWriter, m: SetAdjacentCustomStackingHeightIn) = writeSetAdjacentCustomStackingHeightIn(w, m)

    private fun readSetAdjacentCustomStackingHeightIn(r: PacketReader): SetAdjacentCustomStackingHeightIn {
        val vItemId = r.int()
        val vMoveDown = r.boolean()
        return SetAdjacentCustomStackingHeightIn(vItemId, vMoveDown)
    }

    private fun writeSetAdjacentCustomStackingHeightIn(w: PacketWriter, m: SetAdjacentCustomStackingHeightIn) {
        w.int(m.itemId)
        w.boolean(m.moveDown)
    }
}
