package zel.protocol.generated

data class BotRemovedFromInventoryOut(
    val botId: Int,
)

object BotRemovedFromInventoryOutCodec : OutgoingCodec<BotRemovedFromInventoryOut> {
    override val name = "BotRemovedFromInventoryOut"

    override fun read(r: PacketReader): BotRemovedFromInventoryOut = readBotRemovedFromInventoryOut(r)

    override fun write(w: PacketWriter, m: BotRemovedFromInventoryOut) = writeBotRemovedFromInventoryOut(w, m)

    private fun readBotRemovedFromInventoryOut(r: PacketReader): BotRemovedFromInventoryOut {
        val vBotId = r.int()
        return BotRemovedFromInventoryOut(vBotId)
    }

    private fun writeBotRemovedFromInventoryOut(w: PacketWriter, m: BotRemovedFromInventoryOut) {
        w.int(m.botId)
    }
}
