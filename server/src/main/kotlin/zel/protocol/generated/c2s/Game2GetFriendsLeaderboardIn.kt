package zel.protocol.generated

data class Game2GetFriendsLeaderboardIn(
    val gameType: Int,
    val startRank: Int,
    val direction: Int,
    val viewSize: Int,
    val windowSize: Int,
)

object Game2GetFriendsLeaderboardInCodec : IncomingCodec<Game2GetFriendsLeaderboardIn> {
    override val name = "Game2GetFriendsLeaderboardIn"

    override fun read(r: PacketReader): Game2GetFriendsLeaderboardIn = readGame2GetFriendsLeaderboardIn(r)

    override fun write(w: PacketWriter, m: Game2GetFriendsLeaderboardIn) = writeGame2GetFriendsLeaderboardIn(w, m)

    private fun readGame2GetFriendsLeaderboardIn(r: PacketReader): Game2GetFriendsLeaderboardIn {
        val vGameType = r.int()
        val vStartRank = r.int()
        val vDirection = r.int()
        val vViewSize = r.int()
        val vWindowSize = r.int()
        return Game2GetFriendsLeaderboardIn(vGameType, vStartRank, vDirection, vViewSize, vWindowSize)
    }

    private fun writeGame2GetFriendsLeaderboardIn(w: PacketWriter, m: Game2GetFriendsLeaderboardIn) {
        w.int(m.gameType)
        w.int(m.startRank)
        w.int(m.direction)
        w.int(m.viewSize)
        w.int(m.windowSize)
    }
}
