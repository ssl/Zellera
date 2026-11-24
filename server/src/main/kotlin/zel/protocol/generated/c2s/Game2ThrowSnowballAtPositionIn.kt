package zel.protocol.generated

data class Game2ThrowSnowballAtPositionIn(
    val x: Int,
    val y: Int,
    val trajectory: Int,
    val turn: Int,
    val subturn: Int,
)

object Game2ThrowSnowballAtPositionInCodec : IncomingCodec<Game2ThrowSnowballAtPositionIn> {
    override val name = "Game2ThrowSnowballAtPositionIn"

    override fun read(r: PacketReader): Game2ThrowSnowballAtPositionIn = readGame2ThrowSnowballAtPositionIn(r)

    override fun write(w: PacketWriter, m: Game2ThrowSnowballAtPositionIn) = writeGame2ThrowSnowballAtPositionIn(w, m)

    private fun readGame2ThrowSnowballAtPositionIn(r: PacketReader): Game2ThrowSnowballAtPositionIn {
        val vX = r.int()
        val vY = r.int()
        val vTrajectory = r.int()
        val vTurn = r.int()
        val vSubturn = r.int()
        return Game2ThrowSnowballAtPositionIn(vX, vY, vTrajectory, vTurn, vSubturn)
    }

    private fun writeGame2ThrowSnowballAtPositionIn(w: PacketWriter, m: Game2ThrowSnowballAtPositionIn) {
        w.int(m.x)
        w.int(m.y)
        w.int(m.trajectory)
        w.int(m.turn)
        w.int(m.subturn)
    }
}
