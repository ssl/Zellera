package zel.protocol.generated

data class DefaultSanctionIn(
    val int1: Int,
    val int2: Int,
    val string1: String,
    val int3: Int,
)

object DefaultSanctionInCodec : IncomingCodec<DefaultSanctionIn> {
    override val name = "DefaultSanctionIn"

    override fun read(r: PacketReader): DefaultSanctionIn = readDefaultSanctionIn(r)

    override fun write(w: PacketWriter, m: DefaultSanctionIn) = writeDefaultSanctionIn(w, m)

    private fun readDefaultSanctionIn(r: PacketReader): DefaultSanctionIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        return DefaultSanctionIn(vInt1, vInt2, vString1, vInt3)
    }

    private fun writeDefaultSanctionIn(w: PacketWriter, m: DefaultSanctionIn) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
    }
}
