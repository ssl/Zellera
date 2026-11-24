package zel.protocol.generated

data class ModeratorPlayerInfoOut(
    val int1: Int,
    val string1: String,
    val string2: String,
    val int2: Int,
    val int3: Int,
    val boolean1: Boolean,
    val int4: Int,
    val int5: Int,
    val int6: Int,
    val int7: Int,
    val int8: Int,
    val string3: String,
    val string4: String,
    val int9: Int,
    val int10: Int,
    val string5: String,
    val string6: String,
    val trailing1: Trailing1?,
) {
    data class Trailing1(
        val string1: String,
        val int1: Int,
    )
}

object ModeratorPlayerInfoOutCodec : OutgoingCodec<ModeratorPlayerInfoOut> {
    override val name = "ModeratorPlayerInfoOut"

    override fun read(r: PacketReader): ModeratorPlayerInfoOut = readModeratorPlayerInfoOut(r)

    override fun write(w: PacketWriter, m: ModeratorPlayerInfoOut) = writeModeratorPlayerInfoOut(w, m)

    private fun readModeratorPlayerInfoOutTrailing1(r: PacketReader): ModeratorPlayerInfoOut.Trailing1 {
        val vString1 = r.string()
        val vInt1 = r.int()
        return ModeratorPlayerInfoOut.Trailing1(vString1, vInt1)
    }

    private fun readModeratorPlayerInfoOut(r: PacketReader): ModeratorPlayerInfoOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vBoolean1 = r.boolean()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vString3 = r.string()
        val vString4 = r.string()
        val vInt9 = r.int()
        val vInt10 = r.int()
        val vString5 = r.string()
        val vString6 = r.string()
        val vTrailing1 = if (r.remaining > 0) readModeratorPlayerInfoOutTrailing1(r) else null
        return ModeratorPlayerInfoOut(vInt1, vString1, vString2, vInt2, vInt3, vBoolean1, vInt4, vInt5, vInt6, vInt7, vInt8, vString3, vString4, vInt9, vInt10, vString5, vString6, vTrailing1)
    }

    private fun writeModeratorPlayerInfoOutTrailing1(w: PacketWriter, m: ModeratorPlayerInfoOut.Trailing1) {
        w.string(m.string1)
        w.int(m.int1)
    }

    private fun writeModeratorPlayerInfoOut(w: PacketWriter, m: ModeratorPlayerInfoOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int2)
        w.int(m.int3)
        w.boolean(m.boolean1)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.int(m.int8)
        w.string(m.string3)
        w.string(m.string4)
        w.int(m.int9)
        w.int(m.int10)
        w.string(m.string5)
        w.string(m.string6)
        m.trailing1?.let { writeModeratorPlayerInfoOutTrailing1(w, it) }
    }
}
