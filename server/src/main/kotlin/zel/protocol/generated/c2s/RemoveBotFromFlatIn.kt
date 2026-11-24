package zel.protocol.generated

data class RemoveBotFromFlatIn(
    val botId: Int,
)

object RemoveBotFromFlatInCodec : IncomingCodec<RemoveBotFromFlatIn> {
    override val name = "RemoveBotFromFlatIn"

    override fun read(r: PacketReader): RemoveBotFromFlatIn = readRemoveBotFromFlatIn(r)

    override fun write(w: PacketWriter, m: RemoveBotFromFlatIn) = writeRemoveBotFromFlatIn(w, m)

    private fun readRemoveBotFromFlatIn(r: PacketReader): RemoveBotFromFlatIn {
        val vBotId = r.int()
        return RemoveBotFromFlatIn(vBotId)
    }

    private fun writeRemoveBotFromFlatIn(w: PacketWriter, m: RemoveBotFromFlatIn) {
        w.int(m.botId)
    }
}
