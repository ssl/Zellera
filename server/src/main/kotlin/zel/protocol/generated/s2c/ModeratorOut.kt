package zel.protocol.generated

data class ModeratorOut(
    val string1: String,
    val string2: String,
)

object ModeratorOutCodec : OutgoingCodec<ModeratorOut> {
    override val name = "ModeratorOut"

    override fun read(r: PacketReader): ModeratorOut = readModeratorOut(r)

    override fun write(w: PacketWriter, m: ModeratorOut) = writeModeratorOut(w, m)

    private fun readModeratorOut(r: PacketReader): ModeratorOut {
        val vString1 = r.string()
        val vString2 = r.string()
        return ModeratorOut(vString1, vString2)
    }

    private fun writeModeratorOut(w: PacketWriter, m: ModeratorOut) {
        w.string(m.string1)
        w.string(m.string2)
    }
}
