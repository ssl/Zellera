package zel.protocol.generated

data class YouAreOwnerOut(
    val int1: Int,
)

object YouAreOwnerOutCodec : OutgoingCodec<YouAreOwnerOut> {
    override val name = "YouAreOwnerOut"

    override fun read(r: PacketReader): YouAreOwnerOut = readYouAreOwnerOut(r)

    override fun write(w: PacketWriter, m: YouAreOwnerOut) = writeYouAreOwnerOut(w, m)

    private fun readYouAreOwnerOut(r: PacketReader): YouAreOwnerOut {
        val vInt1 = r.int()
        return YouAreOwnerOut(vInt1)
    }

    private fun writeYouAreOwnerOut(w: PacketWriter, m: YouAreOwnerOut) {
        w.int(m.int1)
    }
}
