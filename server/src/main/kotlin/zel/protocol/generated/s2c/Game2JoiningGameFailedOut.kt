package zel.protocol.generated

data class Game2JoiningGameFailedOut(
    val int1: Int,
)

object Game2JoiningGameFailedOutCodec : OutgoingCodec<Game2JoiningGameFailedOut> {
    override val name = "Game2JoiningGameFailedOut"

    override fun read(r: PacketReader): Game2JoiningGameFailedOut = readGame2JoiningGameFailedOut(r)

    override fun write(w: PacketWriter, m: Game2JoiningGameFailedOut) = writeGame2JoiningGameFailedOut(w, m)

    private fun readGame2JoiningGameFailedOut(r: PacketReader): Game2JoiningGameFailedOut {
        val vInt1 = r.int()
        return Game2JoiningGameFailedOut(vInt1)
    }

    private fun writeGame2JoiningGameFailedOut(w: PacketWriter, m: Game2JoiningGameFailedOut) {
        w.int(m.int1)
    }
}
