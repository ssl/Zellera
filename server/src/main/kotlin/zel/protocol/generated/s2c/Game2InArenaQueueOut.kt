package zel.protocol.generated

data class Game2InArenaQueueOut(
    val int1: Int,
)

object Game2InArenaQueueOutCodec : OutgoingCodec<Game2InArenaQueueOut> {
    override val name = "Game2InArenaQueueOut"

    override fun read(r: PacketReader): Game2InArenaQueueOut = readGame2InArenaQueueOut(r)

    override fun write(w: PacketWriter, m: Game2InArenaQueueOut) = writeGame2InArenaQueueOut(w, m)

    private fun readGame2InArenaQueueOut(r: PacketReader): Game2InArenaQueueOut {
        val vInt1 = r.int()
        return Game2InArenaQueueOut(vInt1)
    }

    private fun writeGame2InArenaQueueOut(w: PacketWriter, m: Game2InArenaQueueOut) {
        w.int(m.int1)
    }
}
