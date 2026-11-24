package zel.protocol.generated

data class Game2SetPlayerMoveTargetIn(
    val x: Int,
    val y: Int,
    val turn: Int,
    val subturn: Int,
)

object Game2SetPlayerMoveTargetInCodec : IncomingCodec<Game2SetPlayerMoveTargetIn> {
    override val name = "Game2SetPlayerMoveTargetIn"

    override fun read(r: PacketReader): Game2SetPlayerMoveTargetIn = readGame2SetPlayerMoveTargetIn(r)

    override fun write(w: PacketWriter, m: Game2SetPlayerMoveTargetIn) = writeGame2SetPlayerMoveTargetIn(w, m)

    private fun readGame2SetPlayerMoveTargetIn(r: PacketReader): Game2SetPlayerMoveTargetIn {
        val vX = r.int()
        val vY = r.int()
        val vTurn = r.int()
        val vSubturn = r.int()
        return Game2SetPlayerMoveTargetIn(vX, vY, vTurn, vSubturn)
    }

    private fun writeGame2SetPlayerMoveTargetIn(w: PacketWriter, m: Game2SetPlayerMoveTargetIn) {
        w.int(m.x)
        w.int(m.y)
        w.int(m.turn)
        w.int(m.subturn)
    }
}
