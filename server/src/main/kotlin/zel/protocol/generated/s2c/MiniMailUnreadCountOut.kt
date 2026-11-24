package zel.protocol.generated

data class MiniMailUnreadCountOut(
    val int1: Int,
)

object MiniMailUnreadCountOutCodec : OutgoingCodec<MiniMailUnreadCountOut> {
    override val name = "MiniMailUnreadCountOut"

    override fun read(r: PacketReader): MiniMailUnreadCountOut = readMiniMailUnreadCountOut(r)

    override fun write(w: PacketWriter, m: MiniMailUnreadCountOut) = writeMiniMailUnreadCountOut(w, m)

    private fun readMiniMailUnreadCountOut(r: PacketReader): MiniMailUnreadCountOut {
        val vInt1 = r.int()
        return MiniMailUnreadCountOut(vInt1)
    }

    private fun writeMiniMailUnreadCountOut(w: PacketWriter, m: MiniMailUnreadCountOut) {
        w.int(m.int1)
    }
}
