package zel.protocol.generated

data class UseFurnitureIn(
    val itemId: Int,
    val state: Int,
)

object UseFurnitureInCodec : IncomingCodec<UseFurnitureIn> {
    override val name = "UseFurnitureIn"

    override fun read(r: PacketReader): UseFurnitureIn = readUseFurnitureIn(r)

    override fun write(w: PacketWriter, m: UseFurnitureIn) = writeUseFurnitureIn(w, m)

    private fun readUseFurnitureIn(r: PacketReader): UseFurnitureIn {
        val vItemId = r.int()
        val vState = r.int()
        return UseFurnitureIn(vItemId, vState)
    }

    private fun writeUseFurnitureIn(w: PacketWriter, m: UseFurnitureIn) {
        w.int(m.itemId)
        w.int(m.state)
    }
}
