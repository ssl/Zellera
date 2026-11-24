package zel.protocol.generated

data class YouAreSpectatorOut(
    val int1: Int,
)

object YouAreSpectatorOutCodec : OutgoingCodec<YouAreSpectatorOut> {
    override val name = "YouAreSpectatorOut"

    override fun read(r: PacketReader): YouAreSpectatorOut = readYouAreSpectatorOut(r)

    override fun write(w: PacketWriter, m: YouAreSpectatorOut) = writeYouAreSpectatorOut(w, m)

    private fun readYouAreSpectatorOut(r: PacketReader): YouAreSpectatorOut {
        val vInt1 = r.int()
        return YouAreSpectatorOut(vInt1)
    }

    private fun writeYouAreSpectatorOut(w: PacketWriter, m: YouAreSpectatorOut) {
        w.int(m.int1)
    }
}
