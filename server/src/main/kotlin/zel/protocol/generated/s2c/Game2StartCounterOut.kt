package zel.protocol.generated

data class Game2StartCounterOut(
    val int1: Int,
)

object Game2StartCounterOutCodec : OutgoingCodec<Game2StartCounterOut> {
    override val name = "Game2StartCounterOut"

    override fun read(r: PacketReader): Game2StartCounterOut = readGame2StartCounterOut(r)

    override fun write(w: PacketWriter, m: Game2StartCounterOut) = writeGame2StartCounterOut(w, m)

    private fun readGame2StartCounterOut(r: PacketReader): Game2StartCounterOut {
        val vInt1 = r.int()
        return Game2StartCounterOut(vInt1)
    }

    private fun writeGame2StartCounterOut(w: PacketWriter, m: Game2StartCounterOut) {
        w.int(m.int1)
    }
}
