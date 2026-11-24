package zel.protocol.generated

data class GetGuildFurniContextMenuInfoIn(
    val itemId: Int,
    val guildId: Int,
)

object GetGuildFurniContextMenuInfoInCodec : IncomingCodec<GetGuildFurniContextMenuInfoIn> {
    override val name = "GetGuildFurniContextMenuInfoIn"

    override fun read(r: PacketReader): GetGuildFurniContextMenuInfoIn = readGetGuildFurniContextMenuInfoIn(r)

    override fun write(w: PacketWriter, m: GetGuildFurniContextMenuInfoIn) = writeGetGuildFurniContextMenuInfoIn(w, m)

    private fun readGetGuildFurniContextMenuInfoIn(r: PacketReader): GetGuildFurniContextMenuInfoIn {
        val vItemId = r.int()
        val vGuildId = r.int()
        return GetGuildFurniContextMenuInfoIn(vItemId, vGuildId)
    }

    private fun writeGetGuildFurniContextMenuInfoIn(w: PacketWriter, m: GetGuildFurniContextMenuInfoIn) {
        w.int(m.itemId)
        w.int(m.guildId)
    }
}
