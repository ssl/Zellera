package zel.protocol.generated

data class WiredTransactionGetRoomLogsIn(
    val pageSize: Int,
    val page: Int,
)

object WiredTransactionGetRoomLogsInCodec : IncomingCodec<WiredTransactionGetRoomLogsIn> {
    override val name = "WiredTransactionGetRoomLogsIn"

    override fun read(r: PacketReader): WiredTransactionGetRoomLogsIn = readWiredTransactionGetRoomLogsIn(r)

    override fun write(w: PacketWriter, m: WiredTransactionGetRoomLogsIn) = writeWiredTransactionGetRoomLogsIn(w, m)

    private fun readWiredTransactionGetRoomLogsIn(r: PacketReader): WiredTransactionGetRoomLogsIn {
        val vPageSize = r.int()
        val vPage = r.int()
        return WiredTransactionGetRoomLogsIn(vPageSize, vPage)
    }

    private fun writeWiredTransactionGetRoomLogsIn(w: PacketWriter, m: WiredTransactionGetRoomLogsIn) {
        w.int(m.pageSize)
        w.int(m.page)
    }
}
