package zel.protocol.generated

data class RoomSettingsDataOut(
    val int1: Int,
    val string1: String,
    val string2: String,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val list1: List<String>,
    val int6: Int,
    val int7: Int,
    val int8: Int,
    val int9: Int,
    val int10: Int,
    val int11: Int,
    val int12: Int,
    val int13: Int,
    val boolean1: Boolean,
    val boolean2: Boolean,
    val int14: Int,
    val boolean3: Boolean,
    val int15: Int,
    val boolean4: Boolean,
    val int16: Int,
    val int17: Int,
    val int18: Int,
    val boolean5: Boolean,
)

object RoomSettingsDataOutCodec : OutgoingCodec<RoomSettingsDataOut> {
    override val name = "RoomSettingsDataOut"

    override fun read(r: PacketReader): RoomSettingsDataOut = readRoomSettingsDataOut(r)

    override fun write(w: PacketWriter, m: RoomSettingsDataOut) = writeRoomSettingsDataOut(w, m)

    private fun readRoomSettingsDataOut(r: PacketReader): RoomSettingsDataOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vList1 = List(r.countInt(2)) { r.string() }
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vInt9 = r.int()
        val vInt10 = r.int()
        val vInt11 = r.int()
        val vInt12 = r.int()
        val vInt13 = r.int()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vInt14 = r.int()
        val vBoolean3 = r.boolean()
        val vInt15 = r.int()
        val vBoolean4 = r.boolean()
        val vInt16 = r.int()
        val vInt17 = r.int()
        val vInt18 = r.int()
        val vBoolean5 = r.boolean()
        return RoomSettingsDataOut(vInt1, vString1, vString2, vInt2, vInt3, vInt4, vInt5, vList1, vInt6, vInt7, vInt8, vInt9, vInt10, vInt11, vInt12, vInt13, vBoolean1, vBoolean2, vInt14, vBoolean3, vInt15, vBoolean4, vInt16, vInt17, vInt18, vBoolean5)
    }

    private fun writeRoomSettingsDataOut(w: PacketWriter, m: RoomSettingsDataOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.countInt(m.list1.size)
        m.list1.forEach { w.string(it) }
        w.int(m.int6)
        w.int(m.int7)
        w.int(m.int8)
        w.int(m.int9)
        w.int(m.int10)
        w.int(m.int11)
        w.int(m.int12)
        w.int(m.int13)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        w.int(m.int14)
        w.boolean(m.boolean3)
        w.int(m.int15)
        w.boolean(m.boolean4)
        w.int(m.int16)
        w.int(m.int17)
        w.int(m.int18)
        w.boolean(m.boolean5)
    }
}
