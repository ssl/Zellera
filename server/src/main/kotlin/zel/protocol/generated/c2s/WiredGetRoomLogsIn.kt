package zel.protocol.generated

data class WiredGetRoomLogsIn(
    val page: Int,
    val pageSize: Int,
    val logLevelFilter: Int,
    val logSourceFilter: Int,
    val query: String,
)

object WiredGetRoomLogsInCodec : IncomingCodec<WiredGetRoomLogsIn> {
    override val name = "WiredGetRoomLogsIn"

    override fun read(r: PacketReader): WiredGetRoomLogsIn = readWiredGetRoomLogsIn(r)

    override fun write(w: PacketWriter, m: WiredGetRoomLogsIn) = writeWiredGetRoomLogsIn(w, m)

    private fun readWiredGetRoomLogsIn(r: PacketReader): WiredGetRoomLogsIn {
        val vPage = r.int()
        val vPageSize = r.int()
        val vLogLevelFilter = r.int()
        val vLogSourceFilter = r.int()
        val vQuery = r.string()
        return WiredGetRoomLogsIn(vPage, vPageSize, vLogLevelFilter, vLogSourceFilter, vQuery)
    }

    private fun writeWiredGetRoomLogsIn(w: PacketWriter, m: WiredGetRoomLogsIn) {
        w.int(m.page)
        w.int(m.pageSize)
        w.int(m.logLevelFilter)
        w.int(m.logSourceFilter)
        w.string(m.query)
    }
}
