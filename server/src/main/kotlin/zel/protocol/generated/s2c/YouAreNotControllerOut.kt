package zel.protocol.generated

data class YouAreNotControllerOut(
    val int1: Int,
)

object YouAreNotControllerOutCodec : OutgoingCodec<YouAreNotControllerOut> {
    override val name = "YouAreNotControllerOut"

    override fun read(r: PacketReader): YouAreNotControllerOut = readYouAreNotControllerOut(r)

    override fun write(w: PacketWriter, m: YouAreNotControllerOut) = writeYouAreNotControllerOut(w, m)

    private fun readYouAreNotControllerOut(r: PacketReader): YouAreNotControllerOut {
        val vInt1 = r.int()
        return YouAreNotControllerOut(vInt1)
    }

    private fun writeYouAreNotControllerOut(w: PacketWriter, m: YouAreNotControllerOut) {
        w.int(m.int1)
    }
}
