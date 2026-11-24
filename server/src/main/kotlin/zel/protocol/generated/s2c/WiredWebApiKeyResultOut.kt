package zel.protocol.generated

data class WiredWebApiKeyResultOut(
    val int1: Int,
    val boolean1: Boolean,
    val string1: String,
)

object WiredWebApiKeyResultOutCodec : OutgoingCodec<WiredWebApiKeyResultOut> {
    override val name = "WiredWebApiKeyResultOut"

    override fun read(r: PacketReader): WiredWebApiKeyResultOut = readWiredWebApiKeyResultOut(r)

    override fun write(w: PacketWriter, m: WiredWebApiKeyResultOut) = writeWiredWebApiKeyResultOut(w, m)

    private fun readWiredWebApiKeyResultOut(r: PacketReader): WiredWebApiKeyResultOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        val vString1 = r.string()
        return WiredWebApiKeyResultOut(vInt1, vBoolean1, vString1)
    }

    private fun writeWiredWebApiKeyResultOut(w: PacketWriter, m: WiredWebApiKeyResultOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
        w.string(m.string1)
    }
}
