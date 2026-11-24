package zel.protocol.generated

data class RelationshipStatusInfoOut(
    val int1: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val int3: Int,
        val string1: String,
        val string2: String,
    )
}

object RelationshipStatusInfoOutCodec : OutgoingCodec<RelationshipStatusInfoOut> {
    override val name = "RelationshipStatusInfoOut"

    override fun read(r: PacketReader): RelationshipStatusInfoOut = readRelationshipStatusInfoOut(r)

    override fun write(w: PacketWriter, m: RelationshipStatusInfoOut) = writeRelationshipStatusInfoOut(w, m)

    private fun readRelationshipStatusInfoOutList1Item(r: PacketReader): RelationshipStatusInfoOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        return RelationshipStatusInfoOut.List1Item(vInt1, vInt2, vInt3, vString1, vString2)
    }

    private fun readRelationshipStatusInfoOut(r: PacketReader): RelationshipStatusInfoOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(16)) { readRelationshipStatusInfoOutList1Item(r) }
        return RelationshipStatusInfoOut(vInt1, vList1)
    }

    private fun writeRelationshipStatusInfoOutList1Item(w: PacketWriter, m: RelationshipStatusInfoOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
        w.string(m.string2)
    }

    private fun writeRelationshipStatusInfoOut(w: PacketWriter, m: RelationshipStatusInfoOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeRelationshipStatusInfoOutList1Item(w, it) }
    }
}
