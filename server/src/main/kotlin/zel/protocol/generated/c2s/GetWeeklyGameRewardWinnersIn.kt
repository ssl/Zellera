package zel.protocol.generated

data class GetWeeklyGameRewardWinnersIn(
    val int1: Int,
)

object GetWeeklyGameRewardWinnersInCodec : IncomingCodec<GetWeeklyGameRewardWinnersIn> {
    override val name = "GetWeeklyGameRewardWinnersIn"

    override fun read(r: PacketReader): GetWeeklyGameRewardWinnersIn = readGetWeeklyGameRewardWinnersIn(r)

    override fun write(w: PacketWriter, m: GetWeeklyGameRewardWinnersIn) = writeGetWeeklyGameRewardWinnersIn(w, m)

    private fun readGetWeeklyGameRewardWinnersIn(r: PacketReader): GetWeeklyGameRewardWinnersIn {
        val vInt1 = r.int()
        return GetWeeklyGameRewardWinnersIn(vInt1)
    }

    private fun writeGetWeeklyGameRewardWinnersIn(w: PacketWriter, m: GetWeeklyGameRewardWinnersIn) {
        w.int(m.int1)
    }
}
