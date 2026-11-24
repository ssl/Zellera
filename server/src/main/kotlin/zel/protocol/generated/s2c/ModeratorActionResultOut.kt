package zel.protocol.generated

data class ModeratorActionResultOut(
    val int1: Int,
    val boolean1: Boolean,
)

object ModeratorActionResultOutCodec : OutgoingCodec<ModeratorActionResultOut> {
    override val name = "ModeratorActionResultOut"

    override fun read(r: PacketReader): ModeratorActionResultOut = readModeratorActionResultOut(r)

    override fun write(w: PacketWriter, m: ModeratorActionResultOut) = writeModeratorActionResultOut(w, m)

    private fun readModeratorActionResultOut(r: PacketReader): ModeratorActionResultOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        return ModeratorActionResultOut(vInt1, vBoolean1)
    }

    private fun writeModeratorActionResultOut(w: PacketWriter, m: ModeratorActionResultOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
    }
}
