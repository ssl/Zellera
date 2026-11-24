package zel.protocol.generated

data class CommunityGoalHallOfFameOut(
    val code: String,
    val winners: List<WinnersItem>,
) {
    data class WinnersItem(
        val userId: Int,
        val userName: String,
        val figure: String,
        val rank: Int,
        val score: Int,
    )
}

object CommunityGoalHallOfFameOutCodec : OutgoingCodec<CommunityGoalHallOfFameOut> {
    override val name = "CommunityGoalHallOfFameOut"

    override fun read(r: PacketReader): CommunityGoalHallOfFameOut = readCommunityGoalHallOfFameOut(r)

    override fun write(w: PacketWriter, m: CommunityGoalHallOfFameOut) = writeCommunityGoalHallOfFameOut(w, m)

    private fun readCommunityGoalHallOfFameOutWinnersItem(r: PacketReader): CommunityGoalHallOfFameOut.WinnersItem {
        val vUserId = r.int()
        val vUserName = r.string()
        val vFigure = r.string()
        val vRank = r.int()
        val vScore = r.int()
        return CommunityGoalHallOfFameOut.WinnersItem(vUserId, vUserName, vFigure, vRank, vScore)
    }

    private fun readCommunityGoalHallOfFameOut(r: PacketReader): CommunityGoalHallOfFameOut {
        val vCode = r.string()
        val vWinners = List(r.countInt(16)) { readCommunityGoalHallOfFameOutWinnersItem(r) }
        return CommunityGoalHallOfFameOut(vCode, vWinners)
    }

    private fun writeCommunityGoalHallOfFameOutWinnersItem(w: PacketWriter, m: CommunityGoalHallOfFameOut.WinnersItem) {
        w.int(m.userId)
        w.string(m.userName)
        w.string(m.figure)
        w.int(m.rank)
        w.int(m.score)
    }

    private fun writeCommunityGoalHallOfFameOut(w: PacketWriter, m: CommunityGoalHallOfFameOut) {
        w.string(m.code)
        w.countInt(m.winners.size)
        m.winners.forEach { writeCommunityGoalHallOfFameOutWinnersItem(w, it) }
    }
}
