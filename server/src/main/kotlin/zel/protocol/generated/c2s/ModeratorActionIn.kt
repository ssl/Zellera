package zel.protocol.generated

data class ModeratorActionIn(
    val int1: Int,
    val string1: String,
    val string2: String,
)

object ModeratorActionInCodec : IncomingCodec<ModeratorActionIn> {
    override val name = "ModeratorActionIn"

    override fun read(r: PacketReader): ModeratorActionIn = readModeratorActionIn(r)

    override fun write(w: PacketWriter, m: ModeratorActionIn) = writeModeratorActionIn(w, m)

    private fun readModeratorActionIn(r: PacketReader): ModeratorActionIn {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        return ModeratorActionIn(vInt1, vString1, vString2)
    }

    private fun writeModeratorActionIn(w: PacketWriter, m: ModeratorActionIn) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
    }
}
