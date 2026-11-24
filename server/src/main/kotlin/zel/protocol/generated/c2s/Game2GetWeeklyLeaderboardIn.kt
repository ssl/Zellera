package zel.protocol.generated

data class Game2GetWeeklyLeaderboardIn(
    val gameType: Int,
    val weekOffset: Int,
    val startRank: Int,
    val direction: Int,
    val viewSize: Int,
    val windowSize: Int,
)

object Game2GetWeeklyLeaderboardInCodec : IncomingCodec<Game2GetWeeklyLeaderboardIn> {
    override val name = "Game2GetWeeklyLeaderboardIn"

    override fun read(r: PacketReader): Game2GetWeeklyLeaderboardIn = readGame2GetWeeklyLeaderboardIn(r)

    override fun write(w: PacketWriter, m: Game2GetWeeklyLeaderboardIn) = writeGame2GetWeeklyLeaderboardIn(w, m)

    private fun readGame2GetWeeklyLeaderboardIn(r: PacketReader): Game2GetWeeklyLeaderboardIn {
        val vGameType = r.int()
        val vWeekOffset = r.int()
        val vStartRank = r.int()
        val vDirection = r.int()
        val vViewSize = r.int()
        val vWindowSize = r.int()
        return Game2GetWeeklyLeaderboardIn(vGameType, vWeekOffset, vStartRank, vDirection, vViewSize, vWindowSize)
    }

    private fun writeGame2GetWeeklyLeaderboardIn(w: PacketWriter, m: Game2GetWeeklyLeaderboardIn) {
        w.int(m.gameType)
        w.int(m.weekOffset)
        w.int(m.startRank)
        w.int(m.direction)
        w.int(m.viewSize)
        w.int(m.windowSize)
    }
}
