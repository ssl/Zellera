package zel.protocol.generated

data class ModMessageIn(
    val int1: Int,
    val string1: String,
    val string2: String,
    val string3: String,
    val int2: Int,
    val trailing1: Int?,
)

object ModMessageInCodec : IncomingCodec<ModMessageIn> {
    override val name = "ModMessageIn"

    override fun read(r: PacketReader): ModMessageIn = readModMessageIn(r)

    override fun write(w: PacketWriter, m: ModMessageIn) = writeModMessageIn(w, m)

    private fun readModMessageIn(r: PacketReader): ModMessageIn {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt2 = r.int()
        val vTrailing1 = if (r.remaining > 0) r.int() else null
        return ModMessageIn(vInt1, vString1, vString2, vString3, vInt2, vTrailing1)
    }

    private fun writeModMessageIn(w: PacketWriter, m: ModMessageIn) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int2)
        m.trailing1?.let { w.int(it) }
    }
}
