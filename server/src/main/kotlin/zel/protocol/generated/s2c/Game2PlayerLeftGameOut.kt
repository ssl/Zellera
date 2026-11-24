package zel.protocol.generated

data class Game2PlayerLeftGameOut(
    val int1: Int,
)

object Game2PlayerLeftGameOutCodec : OutgoingCodec<Game2PlayerLeftGameOut> {
    override val name = "Game2PlayerLeftGameOut"

    override fun read(r: PacketReader): Game2PlayerLeftGameOut = readGame2PlayerLeftGameOut(r)

    override fun write(w: PacketWriter, m: Game2PlayerLeftGameOut) = writeGame2PlayerLeftGameOut(w, m)

    private fun readGame2PlayerLeftGameOut(r: PacketReader): Game2PlayerLeftGameOut {
        val vInt1 = r.int()
        return Game2PlayerLeftGameOut(vInt1)
    }

    private fun writeGame2PlayerLeftGameOut(w: PacketWriter, m: Game2PlayerLeftGameOut) {
        w.int(m.int1)
    }
}
