package zel.protocol.generated

data class GetPlayerGameAchievementsIn(
    val int1: Int,
)

object GetPlayerGameAchievementsInCodec : IncomingCodec<GetPlayerGameAchievementsIn> {
    override val name = "GetPlayerGameAchievementsIn"

    override fun read(r: PacketReader): GetPlayerGameAchievementsIn = readGetPlayerGameAchievementsIn(r)

    override fun write(w: PacketWriter, m: GetPlayerGameAchievementsIn) = writeGetPlayerGameAchievementsIn(w, m)

    private fun readGetPlayerGameAchievementsIn(r: PacketReader): GetPlayerGameAchievementsIn {
        val vInt1 = r.int()
        return GetPlayerGameAchievementsIn(vInt1)
    }

    private fun writeGetPlayerGameAchievementsIn(w: PacketWriter, m: GetPlayerGameAchievementsIn) {
        w.int(m.int1)
    }
}
