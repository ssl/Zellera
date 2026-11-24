package zel.protocol.generated

data class ThrowDiceIn(
    val itemId: Int,
)

object ThrowDiceInCodec : IncomingCodec<ThrowDiceIn> {
    override val name = "ThrowDiceIn"

    override fun read(r: PacketReader): ThrowDiceIn = readThrowDiceIn(r)

    override fun write(w: PacketWriter, m: ThrowDiceIn) = writeThrowDiceIn(w, m)

    private fun readThrowDiceIn(r: PacketReader): ThrowDiceIn {
        val vItemId = r.int()
        return ThrowDiceIn(vItemId)
    }

    private fun writeThrowDiceIn(w: PacketWriter, m: ThrowDiceIn) {
        w.int(m.itemId)
    }
}
