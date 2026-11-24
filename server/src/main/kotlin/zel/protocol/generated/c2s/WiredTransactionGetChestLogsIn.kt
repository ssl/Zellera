package zel.protocol.generated

data class WiredTransactionGetChestLogsIn(
    val chestId: Int,
    val pageSize: Int,
    val page: Int,
)

object WiredTransactionGetChestLogsInCodec : IncomingCodec<WiredTransactionGetChestLogsIn> {
    override val name = "WiredTransactionGetChestLogsIn"

    override fun read(r: PacketReader): WiredTransactionGetChestLogsIn = readWiredTransactionGetChestLogsIn(r)

    override fun write(w: PacketWriter, m: WiredTransactionGetChestLogsIn) = writeWiredTransactionGetChestLogsIn(w, m)

    private fun readWiredTransactionGetChestLogsIn(r: PacketReader): WiredTransactionGetChestLogsIn {
        val vChestId = r.int()
        val vPageSize = r.int()
        val vPage = r.int()
        return WiredTransactionGetChestLogsIn(vChestId, vPageSize, vPage)
    }

    private fun writeWiredTransactionGetChestLogsIn(w: PacketWriter, m: WiredTransactionGetChestLogsIn) {
        w.int(m.chestId)
        w.int(m.pageSize)
        w.int(m.page)
    }
}
