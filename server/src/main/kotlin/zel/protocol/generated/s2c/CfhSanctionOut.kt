package zel.protocol.generated

data class CfhSanctionOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
    val int3: Int,
    val int4: Int,
    val boolean1: Boolean,
    val trailing1: String?,
    val trailing2: String?,
)

object CfhSanctionOutCodec : OutgoingCodec<CfhSanctionOut> {
    override val name = "CfhSanctionOut"

    override fun read(r: PacketReader): CfhSanctionOut = readCfhSanctionOut(r)

    override fun write(w: PacketWriter, m: CfhSanctionOut) = writeCfhSanctionOut(w, m)

    private fun readCfhSanctionOut(r: PacketReader): CfhSanctionOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vBoolean1 = r.boolean()
        val vTrailing1 = if (r.remaining > 0) r.string() else null
        val vTrailing2 = if (r.remaining > 0) r.string() else null
        return CfhSanctionOut(vInt1, vInt2, vString1, vInt3, vInt4, vBoolean1, vTrailing1, vTrailing2)
    }

    private fun writeCfhSanctionOut(w: PacketWriter, m: CfhSanctionOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
        w.int(m.int4)
        w.boolean(m.boolean1)
        m.trailing1?.let { w.string(it) }
        m.trailing2?.let { w.string(it) }
    }
}
