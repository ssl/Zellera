package zel.protocol.generated

data class GetWeeklyGameRewardIn(
    val int1: Int,
)

object GetWeeklyGameRewardInCodec : IncomingCodec<GetWeeklyGameRewardIn> {
    override val name = "GetWeeklyGameRewardIn"

    override fun read(r: PacketReader): GetWeeklyGameRewardIn = readGetWeeklyGameRewardIn(r)

    override fun write(w: PacketWriter, m: GetWeeklyGameRewardIn) = writeGetWeeklyGameRewardIn(w, m)

    private fun readGetWeeklyGameRewardIn(r: PacketReader): GetWeeklyGameRewardIn {
        val vInt1 = r.int()
        return GetWeeklyGameRewardIn(vInt1)
    }

    private fun writeGetWeeklyGameRewardIn(w: PacketWriter, m: GetWeeklyGameRewardIn) {
        w.int(m.int1)
    }
}
