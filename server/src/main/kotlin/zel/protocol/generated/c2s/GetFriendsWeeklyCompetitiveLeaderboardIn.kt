package zel.protocol.generated

data class GetFriendsWeeklyCompetitiveLeaderboardIn(
    val int1: Int,
    val int2: Int,
)

object GetFriendsWeeklyCompetitiveLeaderboardInCodec : IncomingCodec<GetFriendsWeeklyCompetitiveLeaderboardIn> {
    override val name = "GetFriendsWeeklyCompetitiveLeaderboardIn"

    override fun read(r: PacketReader): GetFriendsWeeklyCompetitiveLeaderboardIn = readGetFriendsWeeklyCompetitiveLeaderboardIn(r)

    override fun write(w: PacketWriter, m: GetFriendsWeeklyCompetitiveLeaderboardIn) = writeGetFriendsWeeklyCompetitiveLeaderboardIn(w, m)

    private fun readGetFriendsWeeklyCompetitiveLeaderboardIn(r: PacketReader): GetFriendsWeeklyCompetitiveLeaderboardIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return GetFriendsWeeklyCompetitiveLeaderboardIn(vInt1, vInt2)
    }

    private fun writeGetFriendsWeeklyCompetitiveLeaderboardIn(w: PacketWriter, m: GetFriendsWeeklyCompetitiveLeaderboardIn) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
