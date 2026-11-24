package zel.protocol.generated

data class PlayerGameAchievementsOut(
    val int1: Int,
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

object PlayerGameAchievementsOutCodec : OutgoingCodec<PlayerGameAchievementsOut> {
    override val name = "PlayerGameAchievementsOut"

    override fun read(r: PacketReader): PlayerGameAchievementsOut = readPlayerGameAchievementsOut(r)

    override fun write(w: PacketWriter, m: PlayerGameAchievementsOut) = writePlayerGameAchievementsOut(w, m)

    private fun readPlayerGameAchievementsOutList1Item(r: PacketReader): PlayerGameAchievementsOut.List1Item {
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
        return PlayerGameAchievementsOut.List1Item(vInt1, vInt2, vString1, vInt3, vInt4, vInt5, vInt6, vInt7, vBoolean1, vString2, vString3, vInt8, vInt9, vShort1)
    }

    private fun readPlayerGameAchievementsOut(r: PacketReader): PlayerGameAchievementsOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(45)) { readPlayerGameAchievementsOutList1Item(r) }
        val vString1 = r.string()
        return PlayerGameAchievementsOut(vInt1, vList1, vString1)
    }

    private fun writePlayerGameAchievementsOutList1Item(w: PacketWriter, m: PlayerGameAchievementsOut.List1Item) {
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

    private fun writePlayerGameAchievementsOut(w: PacketWriter, m: PlayerGameAchievementsOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writePlayerGameAchievementsOutList1Item(w, it) }
        w.string(m.string1)
    }
}
