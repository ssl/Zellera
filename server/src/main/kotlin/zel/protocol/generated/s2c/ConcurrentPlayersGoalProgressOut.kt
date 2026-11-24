package zel.protocol.generated

data class ConcurrentPlayersGoalProgressOut(
    val state: Int,
    val userCount: Int,
    val userGoal: Int,
)

object ConcurrentPlayersGoalProgressOutCodec : OutgoingCodec<ConcurrentPlayersGoalProgressOut> {
    override val name = "ConcurrentPlayersGoalProgressOut"

    override fun read(r: PacketReader): ConcurrentPlayersGoalProgressOut = readConcurrentPlayersGoalProgressOut(r)

    override fun write(w: PacketWriter, m: ConcurrentPlayersGoalProgressOut) = writeConcurrentPlayersGoalProgressOut(w, m)

    private fun readConcurrentPlayersGoalProgressOut(r: PacketReader): ConcurrentPlayersGoalProgressOut {
        val vState = r.int()
        val vUserCount = r.int()
        val vUserGoal = r.int()
        return ConcurrentPlayersGoalProgressOut(vState, vUserCount, vUserGoal)
    }

    private fun writeConcurrentPlayersGoalProgressOut(w: PacketWriter, m: ConcurrentPlayersGoalProgressOut) {
        w.int(m.state)
        w.int(m.userCount)
        w.int(m.userGoal)
    }
}
