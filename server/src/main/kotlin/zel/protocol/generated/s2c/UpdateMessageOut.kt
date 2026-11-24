package zel.protocol.generated

data class UpdateMessageOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val string1: String,
    val string2: String,
    val int6: Int,
    val string3: String,
    val byte1: Byte,
    val int7: Int,
    val string4: String,
    val int8: Int,
    val int9: Int,
)

object UpdateMessageOutCodec : OutgoingCodec<UpdateMessageOut> {
    override val name = "UpdateMessageOut"

    override fun read(r: PacketReader): UpdateMessageOut = readUpdateMessageOut(r)

    override fun write(w: PacketWriter, m: UpdateMessageOut) = writeUpdateMessageOut(w, m)

    private fun readUpdateMessageOut(r: PacketReader): UpdateMessageOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt6 = r.int()
        val vString3 = r.string()
        val vByte1 = r.byte()
        val vInt7 = r.int()
        val vString4 = r.string()
        val vInt8 = r.int()
        val vInt9 = r.int()
        return UpdateMessageOut(vInt1, vInt2, vInt3, vInt4, vInt5, vString1, vString2, vInt6, vString3, vByte1, vInt7, vString4, vInt8, vInt9)
    }

    private fun writeUpdateMessageOut(w: PacketWriter, m: UpdateMessageOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int6)
        w.string(m.string3)
        w.byte(m.byte1)
        w.int(m.int7)
        w.string(m.string4)
        w.int(m.int8)
        w.int(m.int9)
    }
}
