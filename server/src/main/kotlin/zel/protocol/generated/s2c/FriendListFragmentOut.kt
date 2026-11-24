package zel.protocol.generated

data class FriendListFragmentOut(
    val int1: Int,
    val int2: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val int2: Int,
        val boolean1: Boolean,
        val boolean2: Boolean,
        val string2: String,
        val int3: Int,
        val string3: String,
        val string4: String,
        val string5: String,
        val boolean3: Boolean,
        val boolean4: Boolean,
        val boolean5: Boolean,
        val short1: Short,
    )
}

object FriendListFragmentOutCodec : OutgoingCodec<FriendListFragmentOut> {
    override val name = "FriendListFragmentOut"

    override fun read(r: PacketReader): FriendListFragmentOut = readFriendListFragmentOut(r)

    override fun write(w: PacketWriter, m: FriendListFragmentOut) = writeFriendListFragmentOut(w, m)

    private fun readFriendListFragmentOutList1Item(r: PacketReader): FriendListFragmentOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vString2 = r.string()
        val vInt3 = r.int()
        val vString3 = r.string()
        val vString4 = r.string()
        val vString5 = r.string()
        val vBoolean3 = r.boolean()
        val vBoolean4 = r.boolean()
        val vBoolean5 = r.boolean()
        val vShort1 = r.short()
        return FriendListFragmentOut.List1Item(vInt1, vString1, vInt2, vBoolean1, vBoolean2, vString2, vInt3, vString3, vString4, vString5, vBoolean3, vBoolean4, vBoolean5, vShort1)
    }

    private fun readFriendListFragmentOut(r: PacketReader): FriendListFragmentOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vList1 = List(r.countInt(29)) { readFriendListFragmentOutList1Item(r) }
        return FriendListFragmentOut(vInt1, vInt2, vList1)
    }

    private fun writeFriendListFragmentOutList1Item(w: PacketWriter, m: FriendListFragmentOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        w.string(m.string2)
        w.int(m.int3)
        w.string(m.string3)
        w.string(m.string4)
        w.string(m.string5)
        w.boolean(m.boolean3)
        w.boolean(m.boolean4)
        w.boolean(m.boolean5)
        w.short(m.short1)
    }

    private fun writeFriendListFragmentOut(w: PacketWriter, m: FriendListFragmentOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.countInt(m.list1.size)
        m.list1.forEach { writeFriendListFragmentOutList1Item(w, it) }
    }
}
