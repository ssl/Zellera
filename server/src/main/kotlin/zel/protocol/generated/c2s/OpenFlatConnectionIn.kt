package zel.protocol.generated

data class OpenFlatConnectionIn(
    val int1: Int,
    val string1: String,
    val int2: Int,
)

object OpenFlatConnectionInCodec : IncomingCodec<OpenFlatConnectionIn> {
    override val name = "OpenFlatConnectionIn"

    override fun read(r: PacketReader): OpenFlatConnectionIn = readOpenFlatConnectionIn(r)

    override fun write(w: PacketWriter, m: OpenFlatConnectionIn) = writeOpenFlatConnectionIn(w, m)

    private fun readOpenFlatConnectionIn(r: PacketReader): OpenFlatConnectionIn {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        return OpenFlatConnectionIn(vInt1, vString1, vInt2)
    }

    private fun writeOpenFlatConnectionIn(w: PacketWriter, m: OpenFlatConnectionIn) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
    }
}
