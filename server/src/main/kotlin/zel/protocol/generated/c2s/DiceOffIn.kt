package zel.protocol.generated

data class DiceOffIn(
    val itemId: Int,
)

object DiceOffInCodec : IncomingCodec<DiceOffIn> {
    override val name = "DiceOffIn"

    override fun read(r: PacketReader): DiceOffIn = readDiceOffIn(r)

    override fun write(w: PacketWriter, m: DiceOffIn) = writeDiceOffIn(w, m)

    private fun readDiceOffIn(r: PacketReader): DiceOffIn {
        val vItemId = r.int()
        return DiceOffIn(vItemId)
    }

    private fun writeDiceOffIn(w: PacketWriter, m: DiceOffIn) {
        w.int(m.itemId)
    }
}
