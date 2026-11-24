package zel.protocol.generated

data class GetDailyQuestIn(
    val easy: Boolean,
    val index: Int,
)

object GetDailyQuestInCodec : IncomingCodec<GetDailyQuestIn> {
    override val name = "GetDailyQuestIn"

    override fun read(r: PacketReader): GetDailyQuestIn = readGetDailyQuestIn(r)

    override fun write(w: PacketWriter, m: GetDailyQuestIn) = writeGetDailyQuestIn(w, m)

    private fun readGetDailyQuestIn(r: PacketReader): GetDailyQuestIn {
        val vEasy = r.boolean()
        val vIndex = r.int()
        return GetDailyQuestIn(vEasy, vIndex)
    }

    private fun writeGetDailyQuestIn(w: PacketWriter, m: GetDailyQuestIn) {
        w.boolean(m.easy)
        w.int(m.index)
    }
}
