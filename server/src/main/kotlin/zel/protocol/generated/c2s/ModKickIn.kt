package zel.protocol.generated

data class ModKickIn(
    val int1: Int,
    val string1: String,
    val int2: Int,
    val int3: Int,
)

object ModKickInCodec : IncomingCodec<ModKickIn> {
    override val name = "ModKickIn"

    override fun read(r: PacketReader): ModKickIn = readModKickIn(r)

    override fun write(w: PacketWriter, m: ModKickIn) = writeModKickIn(w, m)

    private fun readModKickIn(r: PacketReader): ModKickIn {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return ModKickIn(vInt1, vString1, vInt2, vInt3)
    }

    private fun writeModKickIn(w: PacketWriter, m: ModKickIn) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
