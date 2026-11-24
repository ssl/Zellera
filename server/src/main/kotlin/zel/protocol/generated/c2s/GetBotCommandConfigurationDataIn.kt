package zel.protocol.generated

data class GetBotCommandConfigurationDataIn(
    val botId: Int,
    val skillType: Int,
)

object GetBotCommandConfigurationDataInCodec : IncomingCodec<GetBotCommandConfigurationDataIn> {
    override val name = "GetBotCommandConfigurationDataIn"

    override fun read(r: PacketReader): GetBotCommandConfigurationDataIn = readGetBotCommandConfigurationDataIn(r)

    override fun write(w: PacketWriter, m: GetBotCommandConfigurationDataIn) = writeGetBotCommandConfigurationDataIn(w, m)

    private fun readGetBotCommandConfigurationDataIn(r: PacketReader): GetBotCommandConfigurationDataIn {
        val vBotId = r.int()
        val vSkillType = r.int()
        return GetBotCommandConfigurationDataIn(vBotId, vSkillType)
    }

    private fun writeGetBotCommandConfigurationDataIn(w: PacketWriter, m: GetBotCommandConfigurationDataIn) {
        w.int(m.botId)
        w.int(m.skillType)
    }
}
