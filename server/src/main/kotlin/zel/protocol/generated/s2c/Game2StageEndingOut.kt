package zel.protocol.generated

data class Game2StageEndingOut(
    val int1: Int,
)

object Game2StageEndingOutCodec : OutgoingCodec<Game2StageEndingOut> {
    override val name = "Game2StageEndingOut"

    override fun read(r: PacketReader): Game2StageEndingOut = readGame2StageEndingOut(r)

    override fun write(w: PacketWriter, m: Game2StageEndingOut) = writeGame2StageEndingOut(w, m)

    private fun readGame2StageEndingOut(r: PacketReader): Game2StageEndingOut {
        val vInt1 = r.int()
        return Game2StageEndingOut(vInt1)
    }

    private fun writeGame2StageEndingOut(w: PacketWriter, m: Game2StageEndingOut) {
        w.int(m.int1)
    }
}
