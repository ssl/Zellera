package zel.protocol.generated

data class ModifyCustomFilterResultOut(
    val int1: Int,
    val string1: String,
)

object ModifyCustomFilterResultOutCodec : OutgoingCodec<ModifyCustomFilterResultOut> {
    override val name = "ModifyCustomFilterResultOut"

    override fun read(r: PacketReader): ModifyCustomFilterResultOut = readModifyCustomFilterResultOut(r)

    override fun write(w: PacketWriter, m: ModifyCustomFilterResultOut) = writeModifyCustomFilterResultOut(w, m)

    private fun readModifyCustomFilterResultOut(r: PacketReader): ModifyCustomFilterResultOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        return ModifyCustomFilterResultOut(vInt1, vString1)
    }

    private fun writeModifyCustomFilterResultOut(w: PacketWriter, m: ModifyCustomFilterResultOut) {
        w.int(m.int1)
        w.string(m.string1)
    }
}
