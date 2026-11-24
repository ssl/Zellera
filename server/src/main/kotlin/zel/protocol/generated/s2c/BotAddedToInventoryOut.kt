package zel.protocol.generated

data class BotAddedToInventoryOut(
    val botId: Int,
    val name: String,
    val motto: String,
    val gender: String,
    val figure: String,
    val openInventory: Boolean,
)

object BotAddedToInventoryOutCodec : OutgoingCodec<BotAddedToInventoryOut> {
    override val name = "BotAddedToInventoryOut"

    override fun read(r: PacketReader): BotAddedToInventoryOut = readBotAddedToInventoryOut(r)

    override fun write(w: PacketWriter, m: BotAddedToInventoryOut) = writeBotAddedToInventoryOut(w, m)

    private fun readBotAddedToInventoryOut(r: PacketReader): BotAddedToInventoryOut {
        val vBotId = r.int()
        val vName = r.string()
        val vMotto = r.string()
        val vGender = r.string()
        val vFigure = r.string()
        val vOpenInventory = r.boolean()
        return BotAddedToInventoryOut(vBotId, vName, vMotto, vGender, vFigure, vOpenInventory)
    }

    private fun writeBotAddedToInventoryOut(w: PacketWriter, m: BotAddedToInventoryOut) {
        w.int(m.botId)
        w.string(m.name)
        w.string(m.motto)
        w.string(m.gender)
        w.string(m.figure)
        w.boolean(m.openInventory)
    }
}
