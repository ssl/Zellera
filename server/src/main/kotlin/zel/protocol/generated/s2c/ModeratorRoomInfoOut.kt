package zel.protocol.generated

data class ModeratorRoomInfoOut(
    val int1: Int,
    val int2: Int,
    val boolean1: Boolean,
    val int3: Int,
    val string1: String,
    val boolean2: Boolean,
    val string2: String,
    val string3: String,
    val list1: List<String>,
)

object ModeratorRoomInfoOutCodec : OutgoingCodec<ModeratorRoomInfoOut> {
    override val name = "ModeratorRoomInfoOut"

    override fun read(r: PacketReader): ModeratorRoomInfoOut = readModeratorRoomInfoOut(r)

    override fun write(w: PacketWriter, m: ModeratorRoomInfoOut) = writeModeratorRoomInfoOut(w, m)

    private fun readModeratorRoomInfoOut(r: PacketReader): ModeratorRoomInfoOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        val vInt3 = r.int()
        val vString1 = r.string()
        val vBoolean2 = r.boolean()
        val vString2 = r.string()
        val vString3 = r.string()
        val vList1 = List(r.countInt(2)) { r.string() }
        return ModeratorRoomInfoOut(vInt1, vInt2, vBoolean1, vInt3, vString1, vBoolean2, vString2, vString3, vList1)
    }

    private fun writeModeratorRoomInfoOut(w: PacketWriter, m: ModeratorRoomInfoOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.boolean(m.boolean1)
        w.int(m.int3)
        w.string(m.string1)
        w.boolean(m.boolean2)
        w.string(m.string2)
        w.string(m.string3)
        w.countInt(m.list1.size)
        m.list1.forEach { w.string(it) }
    }
}
