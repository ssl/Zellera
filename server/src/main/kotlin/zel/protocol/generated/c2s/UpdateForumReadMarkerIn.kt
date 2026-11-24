package zel.protocol.generated

data class UpdateForumReadMarkerIn(
    val marks: List<MarksItem>,
) {
    data class MarksItem(
        val guildId: Int,
        val messageId: Int,
        val read: Boolean,
    )
}

object UpdateForumReadMarkerInCodec : IncomingCodec<UpdateForumReadMarkerIn> {
    override val name = "UpdateForumReadMarkerIn"

    override fun read(r: PacketReader): UpdateForumReadMarkerIn = readUpdateForumReadMarkerIn(r)

    override fun write(w: PacketWriter, m: UpdateForumReadMarkerIn) = writeUpdateForumReadMarkerIn(w, m)

    private fun readUpdateForumReadMarkerInMarksItem(r: PacketReader): UpdateForumReadMarkerIn.MarksItem {
        val vGuildId = r.int()
        val vMessageId = r.int()
        val vRead = r.boolean()
        return UpdateForumReadMarkerIn.MarksItem(vGuildId, vMessageId, vRead)
    }

    private fun readUpdateForumReadMarkerIn(r: PacketReader): UpdateForumReadMarkerIn {
        val vMarks = List(r.countInt(9)) { readUpdateForumReadMarkerInMarksItem(r) }
        return UpdateForumReadMarkerIn(vMarks)
    }

    private fun writeUpdateForumReadMarkerInMarksItem(w: PacketWriter, m: UpdateForumReadMarkerIn.MarksItem) {
        w.int(m.guildId)
        w.int(m.messageId)
        w.boolean(m.read)
    }

    private fun writeUpdateForumReadMarkerIn(w: PacketWriter, m: UpdateForumReadMarkerIn) {
        w.countInt(m.marks.size)
        m.marks.forEach { writeUpdateForumReadMarkerInMarksItem(w, it) }
    }
}
