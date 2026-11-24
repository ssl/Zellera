package zel.protocol.generated

data class CommandBotIn(
    val botId: Int,
    val skillType: Int,
    val data: String,
)

object CommandBotInCodec : IncomingCodec<CommandBotIn> {
    override val name = "CommandBotIn"

    override fun read(r: PacketReader): CommandBotIn = readCommandBotIn(r)

    override fun write(w: PacketWriter, m: CommandBotIn) = writeCommandBotIn(w, m)

    private fun readCommandBotIn(r: PacketReader): CommandBotIn {
        val vBotId = r.int()
        val vSkillType = r.int()
        val vData = r.string()
        return CommandBotIn(vBotId, vSkillType, vData)
    }

    private fun writeCommandBotIn(w: PacketWriter, m: CommandBotIn) {
        w.int(m.botId)
        w.int(m.skillType)
        w.string(m.data)
    }
}
