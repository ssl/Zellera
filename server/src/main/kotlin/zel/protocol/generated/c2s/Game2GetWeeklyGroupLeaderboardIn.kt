package zel.protocol.generated

data class Game2GetWeeklyGroupLeaderboardIn(
    val gameType: Int,
    val weekOffset: Int,
    val startRank: Int,
    val direction: Int,
    val viewSize: Int,
    val windowSize: Int,
)

object Game2GetWeeklyGroupLeaderboardInCodec : IncomingCodec<Game2GetWeeklyGroupLeaderboardIn> {
    override val name = "Game2GetWeeklyGroupLeaderboardIn"

    override fun read(r: PacketReader): Game2GetWeeklyGroupLeaderboardIn = readGame2GetWeeklyGroupLeaderboardIn(r)

    override fun write(w: PacketWriter, m: Game2GetWeeklyGroupLeaderboardIn) = writeGame2GetWeeklyGroupLeaderboardIn(w, m)

    private fun readGame2GetWeeklyGroupLeaderboardIn(r: PacketReader): Game2GetWeeklyGroupLeaderboardIn {
        val vGameType = r.int()
        val vWeekOffset = r.int()
        val vStartRank = r.int()
        val vDirection = r.int()
        val vViewSize = r.int()
        val vWindowSize = r.int()
        return Game2GetWeeklyGroupLeaderboardIn(vGameType, vWeekOffset, vStartRank, vDirection, vViewSize, vWindowSize)
    }

    private fun writeGame2GetWeeklyGroupLeaderboardIn(w: PacketWriter, m: Game2GetWeeklyGroupLeaderboardIn) {
        w.int(m.gameType)
        w.int(m.weekOffset)
        w.int(m.startRank)
        w.int(m.direction)
        w.int(m.viewSize)
        w.int(m.windowSize)
    }
}
