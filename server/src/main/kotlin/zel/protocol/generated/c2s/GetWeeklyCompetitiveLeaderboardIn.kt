package zel.protocol.generated

data class GetWeeklyCompetitiveLeaderboardIn(
    val int1: Int,
    val int2: Int,
)

object GetWeeklyCompetitiveLeaderboardInCodec : IncomingCodec<GetWeeklyCompetitiveLeaderboardIn> {
    override val name = "GetWeeklyCompetitiveLeaderboardIn"

    override fun read(r: PacketReader): GetWeeklyCompetitiveLeaderboardIn = readGetWeeklyCompetitiveLeaderboardIn(r)

    override fun write(w: PacketWriter, m: GetWeeklyCompetitiveLeaderboardIn) = writeGetWeeklyCompetitiveLeaderboardIn(w, m)

    private fun readGetWeeklyCompetitiveLeaderboardIn(r: PacketReader): GetWeeklyCompetitiveLeaderboardIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return GetWeeklyCompetitiveLeaderboardIn(vInt1, vInt2)
    }

    private fun writeGetWeeklyCompetitiveLeaderboardIn(w: PacketWriter, m: GetWeeklyCompetitiveLeaderboardIn) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
