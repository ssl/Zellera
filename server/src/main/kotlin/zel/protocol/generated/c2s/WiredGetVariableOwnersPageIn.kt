package zel.protocol.generated

data class WiredGetVariableOwnersPageIn(
    val variableId: String,
    val page: Int,
    val pageSize: Int,
    val userTypeFilter: Int,
    val sortTypeFilter: Int,
)

object WiredGetVariableOwnersPageInCodec : IncomingCodec<WiredGetVariableOwnersPageIn> {
    override val name = "WiredGetVariableOwnersPageIn"

    override fun read(r: PacketReader): WiredGetVariableOwnersPageIn = readWiredGetVariableOwnersPageIn(r)

    override fun write(w: PacketWriter, m: WiredGetVariableOwnersPageIn) = writeWiredGetVariableOwnersPageIn(w, m)

    private fun readWiredGetVariableOwnersPageIn(r: PacketReader): WiredGetVariableOwnersPageIn {
        val vVariableId = r.string()
        val vPage = r.int()
        val vPageSize = r.int()
        val vUserTypeFilter = r.int()
        val vSortTypeFilter = r.int()
        return WiredGetVariableOwnersPageIn(vVariableId, vPage, vPageSize, vUserTypeFilter, vSortTypeFilter)
    }

    private fun writeWiredGetVariableOwnersPageIn(w: PacketWriter, m: WiredGetVariableOwnersPageIn) {
        w.string(m.variableId)
        w.int(m.page)
        w.int(m.pageSize)
        w.int(m.userTypeFilter)
        w.int(m.sortTypeFilter)
    }
}
