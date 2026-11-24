package zel.protocol.generated

data class ModBanIn(
    val int1: Int,
    val string1: String,
    val int2: Int,
    val int3: Int,
    val boolean1: Boolean,
    val trailing1: Int?,
)

object ModBanInCodec : IncomingCodec<ModBanIn> {
    override val name = "ModBanIn"

    override fun read(r: PacketReader): ModBanIn = readModBanIn(r)

    override fun write(w: PacketWriter, m: ModBanIn) = writeModBanIn(w, m)

    private fun readModBanIn(r: PacketReader): ModBanIn {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vBoolean1 = r.boolean()
        val vTrailing1 = if (r.remaining > 0) r.int() else null
        return ModBanIn(vInt1, vString1, vInt2, vInt3, vBoolean1, vTrailing1)
    }

    private fun writeModBanIn(w: PacketWriter, m: ModBanIn) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
        w.boolean(m.boolean1)
        m.trailing1?.let { w.int(it) }
    }
}
