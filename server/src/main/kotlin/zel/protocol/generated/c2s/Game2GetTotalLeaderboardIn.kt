package zel.protocol.generated

data class Game2GetTotalLeaderboardIn(
    val gameType: Int,
    val startRank: Int,
    val direction: Int,
    val viewSize: Int,
    val windowSize: Int,
)

object Game2GetTotalLeaderboardInCodec : IncomingCodec<Game2GetTotalLeaderboardIn> {
    override val name = "Game2GetTotalLeaderboardIn"

    override fun read(r: PacketReader): Game2GetTotalLeaderboardIn = readGame2GetTotalLeaderboardIn(r)

    override fun write(w: PacketWriter, m: Game2GetTotalLeaderboardIn) = writeGame2GetTotalLeaderboardIn(w, m)

    private fun readGame2GetTotalLeaderboardIn(r: PacketReader): Game2GetTotalLeaderboardIn {
        val vGameType = r.int()
        val vStartRank = r.int()
        val vDirection = r.int()
        val vViewSize = r.int()
        val vWindowSize = r.int()
        return Game2GetTotalLeaderboardIn(vGameType, vStartRank, vDirection, vViewSize, vWindowSize)
    }

    private fun writeGame2GetTotalLeaderboardIn(w: PacketWriter, m: Game2GetTotalLeaderboardIn) {
        w.int(m.gameType)
        w.int(m.startRank)
        w.int(m.direction)
        w.int(m.viewSize)
        w.int(m.windowSize)
    }
}
