package zel.protocol.generated

data class Game2ThrowSnowballAtHumanIn(
    val targetObjectId: Int,
    val trajectory: Int,
    val turn: Int,
    val subturn: Int,
)

object Game2ThrowSnowballAtHumanInCodec : IncomingCodec<Game2ThrowSnowballAtHumanIn> {
    override val name = "Game2ThrowSnowballAtHumanIn"

    override fun read(r: PacketReader): Game2ThrowSnowballAtHumanIn = readGame2ThrowSnowballAtHumanIn(r)

    override fun write(w: PacketWriter, m: Game2ThrowSnowballAtHumanIn) = writeGame2ThrowSnowballAtHumanIn(w, m)

    private fun readGame2ThrowSnowballAtHumanIn(r: PacketReader): Game2ThrowSnowballAtHumanIn {
        val vTargetObjectId = r.int()
        val vTrajectory = r.int()
        val vTurn = r.int()
        val vSubturn = r.int()
        return Game2ThrowSnowballAtHumanIn(vTargetObjectId, vTrajectory, vTurn, vSubturn)
    }

    private fun writeGame2ThrowSnowballAtHumanIn(w: PacketWriter, m: Game2ThrowSnowballAtHumanIn) {
        w.int(m.targetObjectId)
        w.int(m.trajectory)
        w.int(m.turn)
        w.int(m.subturn)
    }
}
