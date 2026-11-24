package zel.protocol.generated

data class BotInventoryOut(
    val bots: List<BotsItem>,
) {
    data class BotsItem(
        val botId: Int,
        val name: String,
        val motto: String,
        val gender: String,
        val figure: String,
    )
}

object BotInventoryOutCodec : OutgoingCodec<BotInventoryOut> {
    override val name = "BotInventoryOut"

    override fun read(r: PacketReader): BotInventoryOut = readBotInventoryOut(r)

    override fun write(w: PacketWriter, m: BotInventoryOut) = writeBotInventoryOut(w, m)

    private fun readBotInventoryOutBotsItem(r: PacketReader): BotInventoryOut.BotsItem {
        val vBotId = r.int()
        val vName = r.string()
        val vMotto = r.string()
        val vGender = r.string()
        val vFigure = r.string()
        return BotInventoryOut.BotsItem(vBotId, vName, vMotto, vGender, vFigure)
    }

    private fun readBotInventoryOut(r: PacketReader): BotInventoryOut {
        val vBots = List(r.countInt(12)) { readBotInventoryOutBotsItem(r) }
        return BotInventoryOut(vBots)
    }

    private fun writeBotInventoryOutBotsItem(w: PacketWriter, m: BotInventoryOut.BotsItem) {
        w.int(m.botId)
        w.string(m.name)
        w.string(m.motto)
        w.string(m.gender)
        w.string(m.figure)
    }

    private fun writeBotInventoryOut(w: PacketWriter, m: BotInventoryOut) {
        w.countInt(m.bots.size)
        m.bots.forEach { writeBotInventoryOutBotsItem(w, it) }
    }
}
