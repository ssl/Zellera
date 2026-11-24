package zel.protocol.generated

data class Game2StageLoadOut(
    val int1: Int,
)

object Game2StageLoadOutCodec : OutgoingCodec<Game2StageLoadOut> {
    override val name = "Game2StageLoadOut"

    override fun read(r: PacketReader): Game2StageLoadOut = readGame2StageLoadOut(r)

    override fun write(w: PacketWriter, m: Game2StageLoadOut) = writeGame2StageLoadOut(w, m)

    private fun readGame2StageLoadOut(r: PacketReader): Game2StageLoadOut {
        val vInt1 = r.int()
        return Game2StageLoadOut(vInt1)
    }

    private fun writeGame2StageLoadOut(w: PacketWriter, m: Game2StageLoadOut) {
        w.int(m.int1)
    }
}
