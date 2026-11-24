package zel.protocol.generated

data class DiceValueOut(
    val int1: Int,
    val int2: Int,
)

object DiceValueOutCodec : OutgoingCodec<DiceValueOut> {
    override val name = "DiceValueOut"

    override fun read(r: PacketReader): DiceValueOut = readDiceValueOut(r)

    override fun write(w: PacketWriter, m: DiceValueOut) = writeDiceValueOut(w, m)

    private fun readDiceValueOut(r: PacketReader): DiceValueOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return DiceValueOut(vInt1, vInt2)
    }

    private fun writeDiceValueOut(w: PacketWriter, m: DiceValueOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
