package zel.protocol.generated

data class ActivateQuestIn(
    val questId: Int,
)

object ActivateQuestInCodec : IncomingCodec<ActivateQuestIn> {
    override val name = "ActivateQuestIn"

    override fun read(r: PacketReader): ActivateQuestIn = readActivateQuestIn(r)

    override fun write(w: PacketWriter, m: ActivateQuestIn) = writeActivateQuestIn(w, m)

    private fun readActivateQuestIn(r: PacketReader): ActivateQuestIn {
        val vQuestId = r.int()
        return ActivateQuestIn(vQuestId)
    }

    private fun writeActivateQuestIn(w: PacketWriter, m: ActivateQuestIn) {
        w.int(m.questId)
    }
}
