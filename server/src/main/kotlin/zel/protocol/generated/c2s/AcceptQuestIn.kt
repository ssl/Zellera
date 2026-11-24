package zel.protocol.generated

data class AcceptQuestIn(
    val questId: Int,
)

object AcceptQuestInCodec : IncomingCodec<AcceptQuestIn> {
    override val name = "AcceptQuestIn"

    override fun read(r: PacketReader): AcceptQuestIn = readAcceptQuestIn(r)

    override fun write(w: PacketWriter, m: AcceptQuestIn) = writeAcceptQuestIn(w, m)

    private fun readAcceptQuestIn(r: PacketReader): AcceptQuestIn {
        val vQuestId = r.int()
        return AcceptQuestIn(vQuestId)
    }

    private fun writeAcceptQuestIn(w: PacketWriter, m: AcceptQuestIn) {
        w.int(m.questId)
    }
}
