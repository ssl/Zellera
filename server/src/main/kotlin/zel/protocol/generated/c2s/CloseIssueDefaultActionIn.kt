package zel.protocol.generated

data class CloseIssueDefaultActionIn(
    val issueId: Int,
    val issueIds: List<Int>,
    val topic: Int,
)

object CloseIssueDefaultActionInCodec : IncomingCodec<CloseIssueDefaultActionIn> {
    override val name = "CloseIssueDefaultActionIn"

    override fun read(r: PacketReader): CloseIssueDefaultActionIn = readCloseIssueDefaultActionIn(r)

    override fun write(w: PacketWriter, m: CloseIssueDefaultActionIn) = writeCloseIssueDefaultActionIn(w, m)

    private fun readCloseIssueDefaultActionIn(r: PacketReader): CloseIssueDefaultActionIn {
        val vIssueId = r.int()
        val vIssueIds = List(r.countInt(4)) { r.int() }
        val vTopic = r.int()
        return CloseIssueDefaultActionIn(vIssueId, vIssueIds, vTopic)
    }

    private fun writeCloseIssueDefaultActionIn(w: PacketWriter, m: CloseIssueDefaultActionIn) {
        w.int(m.issueId)
        w.countInt(m.issueIds.size)
        m.issueIds.forEach { w.int(it) }
        w.int(m.topic)
    }
}
