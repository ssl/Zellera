package zel.protocol.generated

data class PetRespectNotificationOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val string1: String,
    val int4: Int,
    val int5: Int,
    val string2: String,
    val int6: Int,
    val list1: List<List1Item>,
    val int7: Int,
    val int8: Int,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val int3: Int,
    )
}

object PetRespectNotificationOutCodec : OutgoingCodec<PetRespectNotificationOut> {
    override val name = "PetRespectNotificationOut"

    override fun read(r: PacketReader): PetRespectNotificationOut = readPetRespectNotificationOut(r)

    override fun write(w: PacketWriter, m: PetRespectNotificationOut) = writePetRespectNotificationOut(w, m)

    private fun readPetRespectNotificationOutList1Item(r: PacketReader): PetRespectNotificationOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return PetRespectNotificationOut.List1Item(vInt1, vInt2, vInt3)
    }

    private fun readPetRespectNotificationOut(r: PacketReader): PetRespectNotificationOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vString2 = r.string()
        val vInt6 = r.int()
        val vList1 = List(r.countInt(12)) { readPetRespectNotificationOutList1Item(r) }
        val vInt7 = r.int()
        val vInt8 = r.int()
        return PetRespectNotificationOut(vInt1, vInt2, vInt3, vString1, vInt4, vInt5, vString2, vInt6, vList1, vInt7, vInt8)
    }

    private fun writePetRespectNotificationOutList1Item(w: PacketWriter, m: PetRespectNotificationOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }

    private fun writePetRespectNotificationOut(w: PacketWriter, m: PetRespectNotificationOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
        w.int(m.int4)
        w.int(m.int5)
        w.string(m.string2)
        w.int(m.int6)
        w.countInt(m.list1.size)
        m.list1.forEach { writePetRespectNotificationOutList1Item(w, it) }
        w.int(m.int7)
        w.int(m.int8)
    }
}
