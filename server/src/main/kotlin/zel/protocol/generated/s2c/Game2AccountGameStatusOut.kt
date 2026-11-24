package zel.protocol.generated

data class Game2AccountGameStatusOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
)

object Game2AccountGameStatusOutCodec : OutgoingCodec<Game2AccountGameStatusOut> {
    override val name = "Game2AccountGameStatusOut"

    override fun read(r: PacketReader): Game2AccountGameStatusOut = readGame2AccountGameStatusOut(r)

    override fun write(w: PacketWriter, m: Game2AccountGameStatusOut) = writeGame2AccountGameStatusOut(w, m)

    private fun readGame2AccountGameStatusOut(r: PacketReader): Game2AccountGameStatusOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return Game2AccountGameStatusOut(vInt1, vInt2, vInt3)
    }

    private fun writeGame2AccountGameStatusOut(w: PacketWriter, m: Game2AccountGameStatusOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
