package zel.protocol.generated

data class Game2GetWeeklyFriendsLeaderboardIn(
    val gameType: Int,
    val weekOffset: Int,
    val startRank: Int,
    val direction: Int,
    val viewSize: Int,
    val windowSize: Int,
)

object Game2GetWeeklyFriendsLeaderboardInCodec : IncomingCodec<Game2GetWeeklyFriendsLeaderboardIn> {
    override val name = "Game2GetWeeklyFriendsLeaderboardIn"

    override fun read(r: PacketReader): Game2GetWeeklyFriendsLeaderboardIn = readGame2GetWeeklyFriendsLeaderboardIn(r)

    override fun write(w: PacketWriter, m: Game2GetWeeklyFriendsLeaderboardIn) = writeGame2GetWeeklyFriendsLeaderboardIn(w, m)

    private fun readGame2GetWeeklyFriendsLeaderboardIn(r: PacketReader): Game2GetWeeklyFriendsLeaderboardIn {
        val vGameType = r.int()
        val vWeekOffset = r.int()
        val vStartRank = r.int()
        val vDirection = r.int()
        val vViewSize = r.int()
        val vWindowSize = r.int()
        return Game2GetWeeklyFriendsLeaderboardIn(vGameType, vWeekOffset, vStartRank, vDirection, vViewSize, vWindowSize)
    }

    private fun writeGame2GetWeeklyFriendsLeaderboardIn(w: PacketWriter, m: Game2GetWeeklyFriendsLeaderboardIn) {
        w.int(m.gameType)
        w.int(m.weekOffset)
        w.int(m.startRank)
        w.int(m.direction)
        w.int(m.viewSize)
        w.int(m.windowSize)
    }
}
