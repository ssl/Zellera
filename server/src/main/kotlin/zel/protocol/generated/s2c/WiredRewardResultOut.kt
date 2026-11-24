package zel.protocol.generated

data class WiredRewardResultOut(
    val int1: Int,
)

object WiredRewardResultOutCodec : OutgoingCodec<WiredRewardResultOut> {
    override val name = "WiredRewardResultOut"

    override fun read(r: PacketReader): WiredRewardResultOut = readWiredRewardResultOut(r)

    override fun write(w: PacketWriter, m: WiredRewardResultOut) = writeWiredRewardResultOut(w, m)

    private fun readWiredRewardResultOut(r: PacketReader): WiredRewardResultOut {
        val vInt1 = r.int()
        return WiredRewardResultOut(vInt1)
    }

    private fun writeWiredRewardResultOut(w: PacketWriter, m: WiredRewardResultOut) {
        w.int(m.int1)
    }
}
