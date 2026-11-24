package zel.protocol.generated

data class ReleaseIssuesIn(
    val issueIds: List<Int>,
)

object ReleaseIssuesInCodec : IncomingCodec<ReleaseIssuesIn> {
    override val name = "ReleaseIssuesIn"

    override fun read(r: PacketReader): ReleaseIssuesIn = readReleaseIssuesIn(r)

    override fun write(w: PacketWriter, m: ReleaseIssuesIn) = writeReleaseIssuesIn(w, m)

    private fun readReleaseIssuesIn(r: PacketReader): ReleaseIssuesIn {
        val vIssueIds = List(r.countInt(4)) { r.int() }
        return ReleaseIssuesIn(vIssueIds)
    }

    private fun writeReleaseIssuesIn(w: PacketWriter, m: ReleaseIssuesIn) {
        w.countInt(m.issueIds.size)
        m.issueIds.forEach { w.int(it) }
    }
}
