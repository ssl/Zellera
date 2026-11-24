package zel.protocol.generated

data class Game2PlayerExitedGameArenaOut(
    val int1: Int,
    val int2: Int,
)

object Game2PlayerExitedGameArenaOutCodec : OutgoingCodec<Game2PlayerExitedGameArenaOut> {
    override val name = "Game2PlayerExitedGameArenaOut"

    override fun read(r: PacketReader): Game2PlayerExitedGameArenaOut = readGame2PlayerExitedGameArenaOut(r)

    override fun write(w: PacketWriter, m: Game2PlayerExitedGameArenaOut) = writeGame2PlayerExitedGameArenaOut(w, m)

    private fun readGame2PlayerExitedGameArenaOut(r: PacketReader): Game2PlayerExitedGameArenaOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return Game2PlayerExitedGameArenaOut(vInt1, vInt2)
    }

    private fun writeGame2PlayerExitedGameArenaOut(w: PacketWriter, m: Game2PlayerExitedGameArenaOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
