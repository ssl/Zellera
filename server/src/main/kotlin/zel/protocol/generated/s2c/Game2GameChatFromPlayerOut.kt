package zel.protocol.generated

data class Game2GameChatFromPlayerOut(
    val int1: Int,
    val string1: String,
)

object Game2GameChatFromPlayerOutCodec : OutgoingCodec<Game2GameChatFromPlayerOut> {
    override val name = "Game2GameChatFromPlayerOut"

    override fun read(r: PacketReader): Game2GameChatFromPlayerOut = readGame2GameChatFromPlayerOut(r)

    override fun write(w: PacketWriter, m: Game2GameChatFromPlayerOut) = writeGame2GameChatFromPlayerOut(w, m)

    private fun readGame2GameChatFromPlayerOut(r: PacketReader): Game2GameChatFromPlayerOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        return Game2GameChatFromPlayerOut(vInt1, vString1)
    }

    private fun writeGame2GameChatFromPlayerOut(w: PacketWriter, m: Game2GameChatFromPlayerOut) {
        w.int(m.int1)
        w.string(m.string1)
    }
}
