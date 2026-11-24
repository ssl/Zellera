package zel.protocol.generated

data class PetInfoOut(
    val int1: Int,
    val string1: String,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val int6: Int,
    val int7: Int,
    val int8: Int,
    val int9: Int,
    val int10: Int,
    val int11: Int,
    val int12: Int,
    val string2: String,
    val int13: Int,
    val boolean1: Boolean,
    val boolean2: Boolean,
    val list1: List<Int>,
    val int14: Int,
    val boolean3: Boolean,
    val boolean4: Boolean,
    val boolean5: Boolean,
    val int15: Int,
    val int16: Int,
    val int17: Int,
    val int18: Int,
    val boolean6: Boolean,
)

object PetInfoOutCodec : OutgoingCodec<PetInfoOut> {
    override val name = "PetInfoOut"

    override fun read(r: PacketReader): PetInfoOut = readPetInfoOut(r)

    override fun write(w: PacketWriter, m: PetInfoOut) = writePetInfoOut(w, m)

    private fun readPetInfoOut(r: PacketReader): PetInfoOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vInt9 = r.int()
        val vInt10 = r.int()
        val vInt11 = r.int()
        val vInt12 = r.int()
        val vString2 = r.string()
        val vInt13 = r.int()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vList1 = List(r.countInt(4)) { r.int() }
        val vInt14 = r.int()
        val vBoolean3 = r.boolean()
        val vBoolean4 = r.boolean()
        val vBoolean5 = r.boolean()
        val vInt15 = r.int()
        val vInt16 = r.int()
        val vInt17 = r.int()
        val vInt18 = r.int()
        val vBoolean6 = r.boolean()
        return PetInfoOut(vInt1, vString1, vInt2, vInt3, vInt4, vInt5, vInt6, vInt7, vInt8, vInt9, vInt10, vInt11, vInt12, vString2, vInt13, vBoolean1, vBoolean2, vList1, vInt14, vBoolean3, vBoolean4, vBoolean5, vInt15, vInt16, vInt17, vInt18, vBoolean6)
    }

    private fun writePetInfoOut(w: PacketWriter, m: PetInfoOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.int(m.int8)
        w.int(m.int9)
        w.int(m.int10)
        w.int(m.int11)
        w.int(m.int12)
        w.string(m.string2)
        w.int(m.int13)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.int(m.int14)
        w.boolean(m.boolean3)
        w.boolean(m.boolean4)
        w.boolean(m.boolean5)
        w.int(m.int15)
        w.int(m.int16)
        w.int(m.int17)
        w.int(m.int18)
        w.boolean(m.boolean6)
    }
}
