package zel.protocol.generated

data class Game2PlayerRematchesOut(
    val int1: Int,
)

object Game2PlayerRematchesOutCodec : OutgoingCodec<Game2PlayerRematchesOut> {
    override val name = "Game2PlayerRematchesOut"

    override fun read(r: PacketReader): Game2PlayerRematchesOut = readGame2PlayerRematchesOut(r)

    override fun write(w: PacketWriter, m: Game2PlayerRematchesOut) = writeGame2PlayerRematchesOut(w, m)

    private fun readGame2PlayerRematchesOut(r: PacketReader): Game2PlayerRematchesOut {
        val vInt1 = r.int()
        return Game2PlayerRematchesOut(vInt1)
    }

    private fun writeGame2PlayerRematchesOut(w: PacketWriter, m: Game2PlayerRematchesOut) {
        w.int(m.int1)
    }
}
