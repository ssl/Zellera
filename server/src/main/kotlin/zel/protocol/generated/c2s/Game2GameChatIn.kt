package zel.protocol.generated

data class Game2GameChatIn(
    val text: String,
)

object Game2GameChatInCodec : IncomingCodec<Game2GameChatIn> {
    override val name = "Game2GameChatIn"

    override fun read(r: PacketReader): Game2GameChatIn = readGame2GameChatIn(r)

    override fun write(w: PacketWriter, m: Game2GameChatIn) = writeGame2GameChatIn(w, m)

    private fun readGame2GameChatIn(r: PacketReader): Game2GameChatIn {
        val vText = r.string()
        return Game2GameChatIn(vText)
    }

    private fun writeGame2GameChatIn(w: PacketWriter, m: Game2GameChatIn) {
        w.string(m.text)
    }
}
