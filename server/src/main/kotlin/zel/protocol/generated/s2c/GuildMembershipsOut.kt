package zel.protocol.generated

data class GuildMembershipsOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val string2: String,
        val string3: String,
        val string4: String,
        val boolean1: Boolean,
        val int2: Int,
        val boolean2: Boolean,
    )
}

object GuildMembershipsOutCodec : OutgoingCodec<GuildMembershipsOut> {
    override val name = "GuildMembershipsOut"

    override fun read(r: PacketReader): GuildMembershipsOut = readGuildMembershipsOut(r)

    override fun write(w: PacketWriter, m: GuildMembershipsOut) = writeGuildMembershipsOut(w, m)

    private fun readGuildMembershipsOutList1Item(r: PacketReader): GuildMembershipsOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        val vString4 = r.string()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        val vBoolean2 = r.boolean()
        return GuildMembershipsOut.List1Item(vInt1, vString1, vString2, vString3, vString4, vBoolean1, vInt2, vBoolean2)
    }

    private fun readGuildMembershipsOut(r: PacketReader): GuildMembershipsOut {
        val vList1 = List(r.countInt(18)) { readGuildMembershipsOutList1Item(r) }
        return GuildMembershipsOut(vList1)
    }

    private fun writeGuildMembershipsOutList1Item(w: PacketWriter, m: GuildMembershipsOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
        w.string(m.string4)
        w.boolean(m.boolean1)
        w.int(m.int2)
        w.boolean(m.boolean2)
    }

    private fun writeGuildMembershipsOut(w: PacketWriter, m: GuildMembershipsOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeGuildMembershipsOutList1Item(w, it) }
    }
}
