package zel.protocol.generated

data class IssueCloseNotificationOut(
    val int1: Int,
    val string1: String,
)

object IssueCloseNotificationOutCodec : OutgoingCodec<IssueCloseNotificationOut> {
    override val name = "IssueCloseNotificationOut"

    override fun read(r: PacketReader): IssueCloseNotificationOut = readIssueCloseNotificationOut(r)

    override fun write(w: PacketWriter, m: IssueCloseNotificationOut) = writeIssueCloseNotificationOut(w, m)

    private fun readIssueCloseNotificationOut(r: PacketReader): IssueCloseNotificationOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        return IssueCloseNotificationOut(vInt1, vString1)
    }

    private fun writeIssueCloseNotificationOut(w: PacketWriter, m: IssueCloseNotificationOut) {
        w.int(m.int1)
        w.string(m.string1)
    }
}
