package zel.protocol.generated

data class GuildFurniContextMenuInfoOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
    val int3: Int,
    val boolean1: Boolean,
    val boolean2: Boolean,
)

object GuildFurniContextMenuInfoOutCodec : OutgoingCodec<GuildFurniContextMenuInfoOut> {
    override val name = "GuildFurniContextMenuInfoOut"

    override fun read(r: PacketReader): GuildFurniContextMenuInfoOut = readGuildFurniContextMenuInfoOut(r)

    override fun write(w: PacketWriter, m: GuildFurniContextMenuInfoOut) = writeGuildFurniContextMenuInfoOut(w, m)

    private fun readGuildFurniContextMenuInfoOut(r: PacketReader): GuildFurniContextMenuInfoOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        return GuildFurniContextMenuInfoOut(vInt1, vInt2, vString1, vInt3, vBoolean1, vBoolean2)
    }

    private fun writeGuildFurniContextMenuInfoOut(w: PacketWriter, m: GuildFurniContextMenuInfoOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
    }
}
