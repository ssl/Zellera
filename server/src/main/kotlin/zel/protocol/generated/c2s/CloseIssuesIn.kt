package zel.protocol.generated

data class CloseIssuesIn(
    val state: Int,
    val issueIds: List<Int>,
)

object CloseIssuesInCodec : IncomingCodec<CloseIssuesIn> {
    override val name = "CloseIssuesIn"

    override fun read(r: PacketReader): CloseIssuesIn = readCloseIssuesIn(r)

    override fun write(w: PacketWriter, m: CloseIssuesIn) = writeCloseIssuesIn(w, m)

    private fun readCloseIssuesIn(r: PacketReader): CloseIssuesIn {
        val vState = r.int()
        val vIssueIds = List(r.countInt(4)) { r.int() }
        return CloseIssuesIn(vState, vIssueIds)
    }

    private fun writeCloseIssuesIn(w: PacketWriter, m: CloseIssuesIn) {
        w.int(m.state)
        w.countInt(m.issueIds.size)
        m.issueIds.forEach { w.int(it) }
    }
}
