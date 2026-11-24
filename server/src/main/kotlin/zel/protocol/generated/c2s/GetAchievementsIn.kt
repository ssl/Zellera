package zel.protocol.generated

data object GetAchievementsIn

object GetAchievementsInCodec : IncomingCodec<GetAchievementsIn> {
    override val name = "GetAchievementsIn"

    override fun read(r: PacketReader): GetAchievementsIn = GetAchievementsIn

    override fun write(w: PacketWriter, m: GetAchievementsIn) = writeGetAchievementsIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetAchievementsIn(w: PacketWriter, m: GetAchievementsIn) {
    }
}
