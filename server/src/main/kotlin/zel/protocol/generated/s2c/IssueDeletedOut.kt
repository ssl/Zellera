package zel.protocol.generated

data class IssueDeletedOut(
    val int1: Int,
)

object IssueDeletedOutCodec : OutgoingCodec<IssueDeletedOut> {
    override val name = "IssueDeletedOut"

    override fun read(r: PacketReader): IssueDeletedOut = readIssueDeletedOut(r)

    override fun write(w: PacketWriter, m: IssueDeletedOut) = writeIssueDeletedOut(w, m)

    private fun readIssueDeletedOut(r: PacketReader): IssueDeletedOut {
        val vInt1 = r.int()
        return IssueDeletedOut(vInt1)
    }

    private fun writeIssueDeletedOut(w: PacketWriter, m: IssueDeletedOut) {
        w.int(m.int1)
    }
}
