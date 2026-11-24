package zel.protocol.generated

data class BotCommandConfigurationOut(
    val botId: Int,
    val skillType: Int,
    val data: String,
)

object BotCommandConfigurationOutCodec : OutgoingCodec<BotCommandConfigurationOut> {
    override val name = "BotCommandConfigurationOut"

    override fun read(r: PacketReader): BotCommandConfigurationOut = readBotCommandConfigurationOut(r)

    override fun write(w: PacketWriter, m: BotCommandConfigurationOut) = writeBotCommandConfigurationOut(w, m)

    private fun readBotCommandConfigurationOut(r: PacketReader): BotCommandConfigurationOut {
        val vBotId = r.int()
        val vSkillType = r.int()
        val vData = r.string()
        return BotCommandConfigurationOut(vBotId, vSkillType, vData)
    }

    private fun writeBotCommandConfigurationOut(w: PacketWriter, m: BotCommandConfigurationOut) {
        w.int(m.botId)
        w.int(m.skillType)
        w.string(m.data)
    }
}
