package zel.protocol.generated

data class Game2StartingGameFailedOut(
    val int1: Int,
)

object Game2StartingGameFailedOutCodec : OutgoingCodec<Game2StartingGameFailedOut> {
    override val name = "Game2StartingGameFailedOut"

    override fun read(r: PacketReader): Game2StartingGameFailedOut = readGame2StartingGameFailedOut(r)

    override fun write(w: PacketWriter, m: Game2StartingGameFailedOut) = writeGame2StartingGameFailedOut(w, m)

    private fun readGame2StartingGameFailedOut(r: PacketReader): Game2StartingGameFailedOut {
        val vInt1 = r.int()
        return Game2StartingGameFailedOut(vInt1)
    }

    private fun writeGame2StartingGameFailedOut(w: PacketWriter, m: Game2StartingGameFailedOut) {
        w.int(m.int1)
    }
}
