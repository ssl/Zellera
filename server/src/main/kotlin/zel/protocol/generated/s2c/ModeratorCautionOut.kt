package zel.protocol.generated

data class ModeratorCautionOut(
    val string1: String,
    val string2: String,
)

object ModeratorCautionOutCodec : OutgoingCodec<ModeratorCautionOut> {
    override val name = "ModeratorCautionOut"

    override fun read(r: PacketReader): ModeratorCautionOut = readModeratorCautionOut(r)

    override fun write(w: PacketWriter, m: ModeratorCautionOut) = writeModeratorCautionOut(w, m)

    private fun readModeratorCautionOut(r: PacketReader): ModeratorCautionOut {
        val vString1 = r.string()
        val vString2 = r.string()
        return ModeratorCautionOut(vString1, vString2)
    }

    private fun writeModeratorCautionOut(w: PacketWriter, m: ModeratorCautionOut) {
        w.string(m.string1)
        w.string(m.string2)
    }
}
