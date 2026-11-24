package zel.protocol.generated

data class BotForceOpenContextMenuOut(
    val int1: Int,
)

object BotForceOpenContextMenuOutCodec : OutgoingCodec<BotForceOpenContextMenuOut> {
    override val name = "BotForceOpenContextMenuOut"

    override fun read(r: PacketReader): BotForceOpenContextMenuOut = readBotForceOpenContextMenuOut(r)

    override fun write(w: PacketWriter, m: BotForceOpenContextMenuOut) = writeBotForceOpenContextMenuOut(w, m)

    private fun readBotForceOpenContextMenuOut(r: PacketReader): BotForceOpenContextMenuOut {
        val vInt1 = r.int()
        return BotForceOpenContextMenuOut(vInt1)
    }

    private fun writeBotForceOpenContextMenuOut(w: PacketWriter, m: BotForceOpenContextMenuOut) {
        w.int(m.int1)
    }
}
