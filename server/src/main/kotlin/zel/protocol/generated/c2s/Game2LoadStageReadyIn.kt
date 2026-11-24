package zel.protocol.generated

data class Game2LoadStageReadyIn(
    val percent: Int,
)

object Game2LoadStageReadyInCodec : IncomingCodec<Game2LoadStageReadyIn> {
    override val name = "Game2LoadStageReadyIn"

    override fun read(r: PacketReader): Game2LoadStageReadyIn = readGame2LoadStageReadyIn(r)

    override fun write(w: PacketWriter, m: Game2LoadStageReadyIn) = writeGame2LoadStageReadyIn(w, m)

    private fun readGame2LoadStageReadyIn(r: PacketReader): Game2LoadStageReadyIn {
        val vPercent = r.int()
        return Game2LoadStageReadyIn(vPercent)
    }

    private fun writeGame2LoadStageReadyIn(w: PacketWriter, m: Game2LoadStageReadyIn) {
        w.int(m.percent)
    }
}
