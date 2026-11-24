package zel.protocol.generated

data class PetLevelNotificationOut(
    val int1: Int,
    val string1: String,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val string2: String,
    val int5: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val int3: Int,
    )
}

object PetLevelNotificationOutCodec : OutgoingCodec<PetLevelNotificationOut> {
    override val name = "PetLevelNotificationOut"

    override fun read(r: PacketReader): PetLevelNotificationOut = readPetLevelNotificationOut(r)

    override fun write(w: PacketWriter, m: PetLevelNotificationOut) = writePetLevelNotificationOut(w, m)

    private fun readPetLevelNotificationOutList1Item(r: PacketReader): PetLevelNotificationOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return PetLevelNotificationOut.List1Item(vInt1, vInt2, vInt3)
    }

    private fun readPetLevelNotificationOut(r: PacketReader): PetLevelNotificationOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vString2 = r.string()
        val vInt5 = r.int()
        val vList1 = List(r.countInt(12)) { readPetLevelNotificationOutList1Item(r) }
        return PetLevelNotificationOut(vInt1, vString1, vInt2, vInt3, vInt4, vString2, vInt5, vList1)
    }

    private fun writePetLevelNotificationOutList1Item(w: PacketWriter, m: PetLevelNotificationOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }

    private fun writePetLevelNotificationOut(w: PacketWriter, m: PetLevelNotificationOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.string(m.string2)
        w.int(m.int5)
        w.countInt(m.list1.size)
        m.list1.forEach { writePetLevelNotificationOutList1Item(w, it) }
    }
}
