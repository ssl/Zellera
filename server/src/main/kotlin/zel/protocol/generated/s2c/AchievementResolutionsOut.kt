package zel.protocol.generated

data class AchievementResolutionsOut(
    val int1: Int,
    val list1: List<List1Item>,
    val int2: Int,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val string1: String,
        val int3: Int,
        val int4: Int,
    )
}

object AchievementResolutionsOutCodec : OutgoingCodec<AchievementResolutionsOut> {
    override val name = "AchievementResolutionsOut"

    override fun read(r: PacketReader): AchievementResolutionsOut = readAchievementResolutionsOut(r)

    override fun write(w: PacketWriter, m: AchievementResolutionsOut) = writeAchievementResolutionsOut(w, m)

    private fun readAchievementResolutionsOutList1Item(r: PacketReader): AchievementResolutionsOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        val vInt4 = r.int()
        return AchievementResolutionsOut.List1Item(vInt1, vInt2, vString1, vInt3, vInt4)
    }

    private fun readAchievementResolutionsOut(r: PacketReader): AchievementResolutionsOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(18)) { readAchievementResolutionsOutList1Item(r) }
        val vInt2 = r.int()
        return AchievementResolutionsOut(vInt1, vList1, vInt2)
    }

    private fun writeAchievementResolutionsOutList1Item(w: PacketWriter, m: AchievementResolutionsOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
        w.int(m.int4)
    }

    private fun writeAchievementResolutionsOut(w: PacketWriter, m: AchievementResolutionsOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeAchievementResolutionsOutList1Item(w, it) }
        w.int(m.int2)
    }
}
