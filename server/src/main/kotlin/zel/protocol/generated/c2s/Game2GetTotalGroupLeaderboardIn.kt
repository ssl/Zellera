package zel.protocol.generated

data class Game2GetTotalGroupLeaderboardIn(
    val gameType: Int,
    val startRank: Int,
    val direction: Int,
    val viewSize: Int,
    val windowSize: Int,
)

object Game2GetTotalGroupLeaderboardInCodec : IncomingCodec<Game2GetTotalGroupLeaderboardIn> {
    override val name = "Game2GetTotalGroupLeaderboardIn"

    override fun read(r: PacketReader): Game2GetTotalGroupLeaderboardIn = readGame2GetTotalGroupLeaderboardIn(r)

    override fun write(w: PacketWriter, m: Game2GetTotalGroupLeaderboardIn) = writeGame2GetTotalGroupLeaderboardIn(w, m)

    private fun readGame2GetTotalGroupLeaderboardIn(r: PacketReader): Game2GetTotalGroupLeaderboardIn {
        val vGameType = r.int()
        val vStartRank = r.int()
        val vDirection = r.int()
        val vViewSize = r.int()
        val vWindowSize = r.int()
        return Game2GetTotalGroupLeaderboardIn(vGameType, vStartRank, vDirection, vViewSize, vWindowSize)
    }

    private fun writeGame2GetTotalGroupLeaderboardIn(w: PacketWriter, m: Game2GetTotalGroupLeaderboardIn) {
        w.int(m.gameType)
        w.int(m.startRank)
        w.int(m.direction)
        w.int(m.viewSize)
        w.int(m.windowSize)
    }
}
