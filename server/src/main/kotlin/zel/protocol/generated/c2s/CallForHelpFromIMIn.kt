package zel.protocol.generated

data class CallForHelpFromIMIn(
    val message: String,
    val topic: Int,
    val userId: Int,
    val lines: List<LinesItem>,
    val reporterName: String,
    val reporterEmail: String,
) {
    data class LinesItem(
        val userId: Int,
        val text: String,
    )
}

object CallForHelpFromIMInCodec : IncomingCodec<CallForHelpFromIMIn> {
    override val name = "CallForHelpFromIMIn"

    override fun read(r: PacketReader): CallForHelpFromIMIn = readCallForHelpFromIMIn(r)

    override fun write(w: PacketWriter, m: CallForHelpFromIMIn) = writeCallForHelpFromIMIn(w, m)

    private fun readCallForHelpFromIMInLinesItem(r: PacketReader): CallForHelpFromIMIn.LinesItem {
        val vUserId = r.int()
        val vText = r.string()
        return CallForHelpFromIMIn.LinesItem(vUserId, vText)
    }

    private fun readCallForHelpFromIMIn(r: PacketReader): CallForHelpFromIMIn {
        val vMessage = r.string()
        val vTopic = r.int()
        val vUserId = r.int()
        val vLines = List(r.countInt(6)) { readCallForHelpFromIMInLinesItem(r) }
        val vReporterName = r.string()
        val vReporterEmail = r.string()
        return CallForHelpFromIMIn(vMessage, vTopic, vUserId, vLines, vReporterName, vReporterEmail)
    }

    private fun writeCallForHelpFromIMInLinesItem(w: PacketWriter, m: CallForHelpFromIMIn.LinesItem) {
        w.int(m.userId)
        w.string(m.text)
    }

    private fun writeCallForHelpFromIMIn(w: PacketWriter, m: CallForHelpFromIMIn) {
        w.string(m.message)
        w.int(m.topic)
        w.int(m.userId)
        w.countInt(m.lines.size)
        m.lines.forEach { writeCallForHelpFromIMInLinesItem(w, it) }
        w.string(m.reporterName)
        w.string(m.reporterEmail)
    }
}
