package zel.protocol.generated

data class BotErrorOut(
    val errorCode: Int,
)

object BotErrorOutCodec : OutgoingCodec<BotErrorOut> {
    override val name = "BotErrorOut"

    override fun read(r: PacketReader): BotErrorOut = readBotErrorOut(r)

    override fun write(w: PacketWriter, m: BotErrorOut) = writeBotErrorOut(w, m)

    private fun readBotErrorOut(r: PacketReader): BotErrorOut {
        val vErrorCode = r.int()
        return BotErrorOut(vErrorCode)
    }

    private fun writeBotErrorOut(w: PacketWriter, m: BotErrorOut) {
        w.int(m.errorCode)
    }
}
