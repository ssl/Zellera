package zel.protocol.generated

data class ModToolSanctionIn(
    val int1: Int,
    val int2: Int,
    val int3: Int,
)

object ModToolSanctionInCodec : IncomingCodec<ModToolSanctionIn> {
    override val name = "ModToolSanctionIn"

    override fun read(r: PacketReader): ModToolSanctionIn = readModToolSanctionIn(r)

    override fun write(w: PacketWriter, m: ModToolSanctionIn) = writeModToolSanctionIn(w, m)

    private fun readModToolSanctionIn(r: PacketReader): ModToolSanctionIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return ModToolSanctionIn(vInt1, vInt2, vInt3)
    }

    private fun writeModToolSanctionIn(w: PacketWriter, m: ModToolSanctionIn) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
