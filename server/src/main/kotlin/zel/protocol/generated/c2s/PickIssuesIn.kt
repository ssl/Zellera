package zel.protocol.generated

data class PickIssuesIn(
    val issueIds: List<Int>,
    val retryEnabled: Boolean,
    val retryCount: Int,
    val reason: String,
)

object PickIssuesInCodec : IncomingCodec<PickIssuesIn> {
    override val name = "PickIssuesIn"

    override fun read(r: PacketReader): PickIssuesIn = readPickIssuesIn(r)

    override fun write(w: PacketWriter, m: PickIssuesIn) = writePickIssuesIn(w, m)

    private fun readPickIssuesIn(r: PacketReader): PickIssuesIn {
        val vIssueIds = List(r.countInt(4)) { r.int() }
        val vRetryEnabled = r.boolean()
        val vRetryCount = r.int()
        val vReason = r.string()
        return PickIssuesIn(vIssueIds, vRetryEnabled, vRetryCount, vReason)
    }

    private fun writePickIssuesIn(w: PacketWriter, m: PickIssuesIn) {
        w.countInt(m.issueIds.size)
        m.issueIds.forEach { w.int(it) }
        w.boolean(m.retryEnabled)
        w.int(m.retryCount)
        w.string(m.reason)
    }
}
