package zel.protocol.generated

data class RejectQuestIn(
    val questId: Int,
)

object RejectQuestInCodec : IncomingCodec<RejectQuestIn> {
    override val name = "RejectQuestIn"

    override fun read(r: PacketReader): RejectQuestIn = readRejectQuestIn(r)

    override fun write(w: PacketWriter, m: RejectQuestIn) = writeRejectQuestIn(w, m)

    private fun readRejectQuestIn(r: PacketReader): RejectQuestIn {
        val vQuestId = r.int()
        return RejectQuestIn(vQuestId)
    }

    private fun writeRejectQuestIn(w: PacketWriter, m: RejectQuestIn) {
        w.int(m.questId)
    }
}
