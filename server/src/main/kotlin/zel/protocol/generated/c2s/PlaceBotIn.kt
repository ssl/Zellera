package zel.protocol.generated

data class PlaceBotIn(
    val botId: Int,
    val x: Int,
    val y: Int,
)

object PlaceBotInCodec : IncomingCodec<PlaceBotIn> {
    override val name = "PlaceBotIn"

    override fun read(r: PacketReader): PlaceBotIn = readPlaceBotIn(r)

    override fun write(w: PacketWriter, m: PlaceBotIn) = writePlaceBotIn(w, m)

    private fun readPlaceBotIn(r: PacketReader): PlaceBotIn {
        val vBotId = r.int()
        val vX = r.int()
        val vY = r.int()
        return PlaceBotIn(vBotId, vX, vY)
    }

    private fun writePlaceBotIn(w: PacketWriter, m: PlaceBotIn) {
        w.int(m.botId)
        w.int(m.x)
        w.int(m.y)
    }
}
