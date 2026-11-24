package zel.protocol.generated

data class PlayerObjectOut(
    val int1: Int,
    val string1: String,
    val string2: String,
    val string3: String,
    val string4: String,
    val string5: String,
    val boolean1: Boolean,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val boolean2: Boolean,
    val string6: String,
    val boolean3: Boolean,
    val boolean4: Boolean,
    val trailing1: Trailing1?,
    val trailing2: Trailing2?,
) {
    data class Trailing1(
        val boolean1: Boolean,
        val string1: String,
    )

    data class Trailing2(
        val int1: Int,
        val int2: Int,
    )
}

object PlayerObjectOutCodec : OutgoingCodec<PlayerObjectOut> {
    override val name = "PlayerObjectOut"

    override fun read(r: PacketReader): PlayerObjectOut = readPlayerObjectOut(r)

    override fun write(w: PacketWriter, m: PlayerObjectOut) = writePlayerObjectOut(w, m)

    private fun readPlayerObjectOutTrailing1(r: PacketReader): PlayerObjectOut.Trailing1 {
        val vBoolean1 = r.boolean()
        val vString1 = r.string()
        return PlayerObjectOut.Trailing1(vBoolean1, vString1)
    }

    private fun readPlayerObjectOutTrailing2(r: PacketReader): PlayerObjectOut.Trailing2 {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return PlayerObjectOut.Trailing2(vInt1, vInt2)
    }

    private fun readPlayerObjectOut(r: PacketReader): PlayerObjectOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        val vString4 = r.string()
        val vString5 = r.string()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vBoolean2 = r.boolean()
        val vString6 = r.string()
        val vBoolean3 = r.boolean()
        val vBoolean4 = r.boolean()
        val vTrailing1 = if (r.remaining > 0) readPlayerObjectOutTrailing1(r) else null
        val vTrailing2 = if (r.remaining > 0) readPlayerObjectOutTrailing2(r) else null
        return PlayerObjectOut(vInt1, vString1, vString2, vString3, vString4, vString5, vBoolean1, vInt2, vInt3, vInt4, vBoolean2, vString6, vBoolean3, vBoolean4, vTrailing1, vTrailing2)
    }

    private fun writePlayerObjectOutTrailing1(w: PacketWriter, m: PlayerObjectOut.Trailing1) {
        w.boolean(m.boolean1)
        w.string(m.string1)
    }

    private fun writePlayerObjectOutTrailing2(w: PacketWriter, m: PlayerObjectOut.Trailing2) {
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writePlayerObjectOut(w: PacketWriter, m: PlayerObjectOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
        w.string(m.string4)
        w.string(m.string5)
        w.boolean(m.boolean1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.boolean(m.boolean2)
        w.string(m.string6)
        w.boolean(m.boolean3)
        w.boolean(m.boolean4)
        m.trailing1?.let { writePlayerObjectOutTrailing1(w, it) }
        m.trailing2?.let { writePlayerObjectOutTrailing2(w, it) }
    }
}
