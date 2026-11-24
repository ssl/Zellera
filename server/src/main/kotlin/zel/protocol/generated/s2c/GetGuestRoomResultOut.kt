package zel.protocol.generated

data class GetGuestRoomResultOut(
    val boolean1: Boolean,
    val int1: Int,
    val string1: String,
    val int2: Int,
    val string2: String,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val string3: String,
    val int6: Int,
    val int7: Int,
    val int8: Int,
    val int9: Int,
    val list1: List<String>,
    val int10: Int,
    val rest: ByteArray,
)

object GetGuestRoomResultOutCodec : OutgoingCodec<GetGuestRoomResultOut> {
    override val name = "GetGuestRoomResultOut"

    override fun read(r: PacketReader): GetGuestRoomResultOut = readGetGuestRoomResultOut(r)

    override fun write(w: PacketWriter, m: GetGuestRoomResultOut) = writeGetGuestRoomResultOut(w, m)

    private fun readGetGuestRoomResultOut(r: PacketReader): GetGuestRoomResultOut {
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vString2 = r.string()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vString3 = r.string()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vInt9 = r.int()
        val vList1 = List(r.countInt(2)) { r.string() }
        val vInt10 = r.int()
        val vRest = r.rest()
        return GetGuestRoomResultOut(vBoolean1, vInt1, vString1, vInt2, vString2, vInt3, vInt4, vInt5, vString3, vInt6, vInt7, vInt8, vInt9, vList1, vInt10, vRest)
    }

    private fun writeGetGuestRoomResultOut(w: PacketWriter, m: GetGuestRoomResultOut) {
        w.boolean(m.boolean1)
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.string(m.string2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.string(m.string3)
        w.int(m.int6)
        w.int(m.int7)
        w.int(m.int8)
        w.int(m.int9)
        w.countInt(m.list1.size)
        m.list1.forEach { w.string(it) }
        w.int(m.int10)
        w.bytes(m.rest)
    }
}
