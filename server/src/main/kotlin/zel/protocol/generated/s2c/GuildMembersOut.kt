package zel.protocol.generated

data class GuildMembersOut(
    val int1: Int,
    val string1: String,
    val int2: Int,
    val string2: String,
    val int3: Int,
    val list1: List<List1Item>,
    val boolean1: Boolean,
    val int4: Int,
    val int5: Int,
    val int6: Int,
    val string3: String,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val string1: String,
        val string2: String,
        val string3: String,
    )
}

object GuildMembersOutCodec : OutgoingCodec<GuildMembersOut> {
    override val name = "GuildMembersOut"

    override fun read(r: PacketReader): GuildMembersOut = readGuildMembersOut(r)

    override fun write(w: PacketWriter, m: GuildMembersOut) = writeGuildMembersOut(w, m)

    private fun readGuildMembersOutList1Item(r: PacketReader): GuildMembersOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        return GuildMembersOut.List1Item(vInt1, vInt2, vString1, vString2, vString3)
    }

    private fun readGuildMembersOut(r: PacketReader): GuildMembersOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vString2 = r.string()
        val vInt3 = r.int()
        val vList1 = List(r.countInt(14)) { readGuildMembersOutList1Item(r) }
        val vBoolean1 = r.boolean()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vString3 = r.string()
        return GuildMembersOut(vInt1, vString1, vInt2, vString2, vInt3, vList1, vBoolean1, vInt4, vInt5, vInt6, vString3)
    }

    private fun writeGuildMembersOutList1Item(w: PacketWriter, m: GuildMembersOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
    }

    private fun writeGuildMembersOut(w: PacketWriter, m: GuildMembersOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.string(m.string2)
        w.int(m.int3)
        w.countInt(m.list1.size)
        m.list1.forEach { writeGuildMembersOutList1Item(w, it) }
        w.boolean(m.boolean1)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.string(m.string3)
    }
}
