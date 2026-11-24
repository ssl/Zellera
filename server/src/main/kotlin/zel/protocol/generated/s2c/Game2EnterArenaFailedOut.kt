package zel.protocol.generated

data class Game2EnterArenaFailedOut(
    val int1: Int,
)

object Game2EnterArenaFailedOutCodec : OutgoingCodec<Game2EnterArenaFailedOut> {
    override val name = "Game2EnterArenaFailedOut"

    override fun read(r: PacketReader): Game2EnterArenaFailedOut = readGame2EnterArenaFailedOut(r)

    override fun write(w: PacketWriter, m: Game2EnterArenaFailedOut) = writeGame2EnterArenaFailedOut(w, m)

    private fun readGame2EnterArenaFailedOut(r: PacketReader): Game2EnterArenaFailedOut {
        val vInt1 = r.int()
        return Game2EnterArenaFailedOut(vInt1)
    }

    private fun writeGame2EnterArenaFailedOut(w: PacketWriter, m: Game2EnterArenaFailedOut) {
        w.int(m.int1)
    }
}
