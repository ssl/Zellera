package zel.protocol.generated

data class GamePlayerValueOut(
    val int1: Int,
    val int2: Int,
)

object GamePlayerValueOutCodec : OutgoingCodec<GamePlayerValueOut> {
    override val name = "GamePlayerValueOut"

    override fun read(r: PacketReader): GamePlayerValueOut = readGamePlayerValueOut(r)

    override fun write(w: PacketWriter, m: GamePlayerValueOut) = writeGamePlayerValueOut(w, m)

    private fun readGamePlayerValueOut(r: PacketReader): GamePlayerValueOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return GamePlayerValueOut(vInt1, vInt2)
    }

    private fun writeGamePlayerValueOut(w: PacketWriter, m: GamePlayerValueOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
