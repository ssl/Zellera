package zel.protocol.generated

data class Game2GameRejoinOut(
    val int1: Int,
)

object Game2GameRejoinOutCodec : OutgoingCodec<Game2GameRejoinOut> {
    override val name = "Game2GameRejoinOut"

    override fun read(r: PacketReader): Game2GameRejoinOut = readGame2GameRejoinOut(r)

    override fun write(w: PacketWriter, m: Game2GameRejoinOut) = writeGame2GameRejoinOut(w, m)

    private fun readGame2GameRejoinOut(r: PacketReader): Game2GameRejoinOut {
        val vInt1 = r.int()
        return Game2GameRejoinOut(vInt1)
    }

    private fun writeGame2GameRejoinOut(w: PacketWriter, m: Game2GameRejoinOut) {
        w.int(m.int1)
    }
}
