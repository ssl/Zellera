package zel.protocol.generated

data class CallForHelpIn(
    val message: String,
    val topic: Int,
    val userId: Int,
    val roomId: Int,
    val lines: List<LinesItem>,
    val reporterName: String,
    val reporterEmail: String,
) {
    data class LinesItem(
        val userId: Int,
        val text: String,
    )
}

object CallForHelpInCodec : IncomingCodec<CallForHelpIn> {
    override val name = "CallForHelpIn"

    override fun read(r: PacketReader): CallForHelpIn = readCallForHelpIn(r)

    override fun write(w: PacketWriter, m: CallForHelpIn) = writeCallForHelpIn(w, m)

    private fun readCallForHelpInLinesItem(r: PacketReader): CallForHelpIn.LinesItem {
        val vUserId = r.int()
        val vText = r.string()
        return CallForHelpIn.LinesItem(vUserId, vText)
    }

    private fun readCallForHelpIn(r: PacketReader): CallForHelpIn {
        val vMessage = r.string()
        val vTopic = r.int()
        val vUserId = r.int()
        val vRoomId = r.int()
        val vLines = List(r.countInt(6)) { readCallForHelpInLinesItem(r) }
        val vReporterName = r.string()
        val vReporterEmail = r.string()
        return CallForHelpIn(vMessage, vTopic, vUserId, vRoomId, vLines, vReporterName, vReporterEmail)
    }

    private fun writeCallForHelpInLinesItem(w: PacketWriter, m: CallForHelpIn.LinesItem) {
        w.int(m.userId)
        w.string(m.text)
    }

    private fun writeCallForHelpIn(w: PacketWriter, m: CallForHelpIn) {
        w.string(m.message)
        w.int(m.topic)
        w.int(m.userId)
        w.int(m.roomId)
        w.countInt(m.lines.size)
        m.lines.forEach { writeCallForHelpInLinesItem(w, it) }
        w.string(m.reporterName)
        w.string(m.reporterEmail)
    }
}
