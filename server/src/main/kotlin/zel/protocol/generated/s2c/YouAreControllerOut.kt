package zel.protocol.generated

data class YouAreControllerOut(
    val int1: Int,
    val int2: Int,
)

object YouAreControllerOutCodec : OutgoingCodec<YouAreControllerOut> {
    override val name = "YouAreControllerOut"

    override fun read(r: PacketReader): YouAreControllerOut = readYouAreControllerOut(r)

    override fun write(w: PacketWriter, m: YouAreControllerOut) = writeYouAreControllerOut(w, m)

    private fun readYouAreControllerOut(r: PacketReader): YouAreControllerOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return YouAreControllerOut(vInt1, vInt2)
    }

    private fun writeYouAreControllerOut(w: PacketWriter, m: YouAreControllerOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
