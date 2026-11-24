package zel.protocol.generated

data class Game2PlayerBlockedOut(
    val int1: Int,
)

object Game2PlayerBlockedOutCodec : OutgoingCodec<Game2PlayerBlockedOut> {
    override val name = "Game2PlayerBlockedOut"

    override fun read(r: PacketReader): Game2PlayerBlockedOut = readGame2PlayerBlockedOut(r)

    override fun write(w: PacketWriter, m: Game2PlayerBlockedOut) = writeGame2PlayerBlockedOut(w, m)

    private fun readGame2PlayerBlockedOut(r: PacketReader): Game2PlayerBlockedOut {
        val vInt1 = r.int()
        return Game2PlayerBlockedOut(vInt1)
    }

    private fun writeGame2PlayerBlockedOut(w: PacketWriter, m: Game2PlayerBlockedOut) {
        w.int(m.int1)
    }
}
