package zel.protocol.generated

data class AchievementsOut(
    val list1: List<List1Item>,
    val string1: String,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val string1: String,
        val int3: Int,
        val int4: Int,
        val int5: Int,
        val int6: Int,
        val int7: Int,
        val boolean1: Boolean,
        val string2: String,
        val string3: String,
        val int8: Int,
        val int9: Int,
        val short1: Short,
    )
}

object AchievementsOutCodec : OutgoingCodec<AchievementsOut> {
    override val name = "AchievementsOut"

    override fun read(r: PacketReader): AchievementsOut = readAchievementsOut(r)

    override fun write(w: PacketWriter, m: AchievementsOut) = writeAchievementsOut(w, m)

    private fun readAchievementsOutList1Item(r: PacketReader): AchievementsOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vBoolean1 = r.boolean()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt8 = r.int()
        val vInt9 = r.int()
        val vShort1 = r.short()
        return AchievementsOut.List1Item(vInt1, vInt2, vString1, vInt3, vInt4, vInt5, vInt6, vInt7, vBoolean1, vString2, vString3, vInt8, vInt9, vShort1)
    }

    private fun readAchievementsOut(r: PacketReader): AchievementsOut {
        val vList1 = List(r.countInt(45)) { readAchievementsOutList1Item(r) }
        val vString1 = r.string()
        return AchievementsOut(vList1, vString1)
    }

    private fun writeAchievementsOutList1Item(w: PacketWriter, m: AchievementsOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.boolean(m.boolean1)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int8)
        w.int(m.int9)
        w.short(m.short1)
    }

    private fun writeAchievementsOut(w: PacketWriter, m: AchievementsOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeAchievementsOutList1Item(w, it) }
        w.string(m.string1)
    }
}
