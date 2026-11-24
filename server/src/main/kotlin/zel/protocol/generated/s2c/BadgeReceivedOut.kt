package zel.protocol.generated

data class BadgeReceivedOut(
    val int1: Int,
    val string1: String,
    val int2: Int,
    val int3: Int,
)

object BadgeReceivedOutCodec : OutgoingCodec<BadgeReceivedOut> {
    override val name = "BadgeReceivedOut"

    override fun read(r: PacketReader): BadgeReceivedOut = readBadgeReceivedOut(r)

    override fun write(w: PacketWriter, m: BadgeReceivedOut) = writeBadgeReceivedOut(w, m)

    private fun readBadgeReceivedOut(r: PacketReader): BadgeReceivedOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return BadgeReceivedOut(vInt1, vString1, vInt2, vInt3)
    }

    private fun writeBadgeReceivedOut(w: PacketWriter, m: BadgeReceivedOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
