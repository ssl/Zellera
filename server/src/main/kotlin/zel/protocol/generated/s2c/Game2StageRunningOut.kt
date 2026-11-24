package zel.protocol.generated

data class Game2StageRunningOut(
    val int1: Int,
)

object Game2StageRunningOutCodec : OutgoingCodec<Game2StageRunningOut> {
    override val name = "Game2StageRunningOut"

    override fun read(r: PacketReader): Game2StageRunningOut = readGame2StageRunningOut(r)

    override fun write(w: PacketWriter, m: Game2StageRunningOut) = writeGame2StageRunningOut(w, m)

    private fun readGame2StageRunningOut(r: PacketReader): Game2StageRunningOut {
        val vInt1 = r.int()
        return Game2StageRunningOut(vInt1)
    }

    private fun writeGame2StageRunningOut(w: PacketWriter, m: Game2StageRunningOut) {
        w.int(m.int1)
    }
}
