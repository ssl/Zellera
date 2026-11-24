package zel.protocol.generated

data class BadgeInfoOut(
    val int1: Int,
    val string1: String,
    val int2: Int,
    val int3: Int,
)

object BadgeInfoOutCodec : OutgoingCodec<BadgeInfoOut> {
    override val name = "BadgeInfoOut"

    override fun read(r: PacketReader): BadgeInfoOut = readBadgeInfoOut(r)

    override fun write(w: PacketWriter, m: BadgeInfoOut) = writeBadgeInfoOut(w, m)

    private fun readBadgeInfoOut(r: PacketReader): BadgeInfoOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return BadgeInfoOut(vInt1, vString1, vInt2, vInt3)
    }

    private fun writeBadgeInfoOut(w: PacketWriter, m: BadgeInfoOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
