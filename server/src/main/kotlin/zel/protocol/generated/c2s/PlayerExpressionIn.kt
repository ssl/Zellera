package zel.protocol.generated

data class PlayerExpressionIn(
    val int1: Int,
)

object PlayerExpressionInCodec : IncomingCodec<PlayerExpressionIn> {
    override val name = "PlayerExpressionIn"

    override fun read(r: PacketReader): PlayerExpressionIn = readPlayerExpressionIn(r)

    override fun write(w: PacketWriter, m: PlayerExpressionIn) = writePlayerExpressionIn(w, m)

    private fun readPlayerExpressionIn(r: PacketReader): PlayerExpressionIn {
        val vInt1 = r.int()
        return PlayerExpressionIn(vInt1)
    }

    private fun writePlayerExpressionIn(w: PacketWriter, m: PlayerExpressionIn) {
        w.int(m.int1)
    }
}
