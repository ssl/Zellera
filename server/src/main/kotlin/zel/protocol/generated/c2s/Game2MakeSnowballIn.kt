package zel.protocol.generated

data class Game2MakeSnowballIn(
    val turn: Int,
    val subturn: Int,
)

object Game2MakeSnowballInCodec : IncomingCodec<Game2MakeSnowballIn> {
    override val name = "Game2MakeSnowballIn"

    override fun read(r: PacketReader): Game2MakeSnowballIn = readGame2MakeSnowballIn(r)

    override fun write(w: PacketWriter, m: Game2MakeSnowballIn) = writeGame2MakeSnowballIn(w, m)

    private fun readGame2MakeSnowballIn(r: PacketReader): Game2MakeSnowballIn {
        val vTurn = r.int()
        val vSubturn = r.int()
        return Game2MakeSnowballIn(vTurn, vSubturn)
    }

    private fun writeGame2MakeSnowballIn(w: PacketWriter, m: Game2MakeSnowballIn) {
        w.int(m.turn)
        w.int(m.subturn)
    }
}
