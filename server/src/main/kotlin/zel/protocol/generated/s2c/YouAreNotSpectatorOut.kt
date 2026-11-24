package zel.protocol.generated

data class YouAreNotSpectatorOut(
    val int1: Int,
)

object YouAreNotSpectatorOutCodec : OutgoingCodec<YouAreNotSpectatorOut> {
    override val name = "YouAreNotSpectatorOut"

    override fun read(r: PacketReader): YouAreNotSpectatorOut = readYouAreNotSpectatorOut(r)

    override fun write(w: PacketWriter, m: YouAreNotSpectatorOut) = writeYouAreNotSpectatorOut(w, m)

    private fun readYouAreNotSpectatorOut(r: PacketReader): YouAreNotSpectatorOut {
        val vInt1 = r.int()
        return YouAreNotSpectatorOut(vInt1)
    }

    private fun writeYouAreNotSpectatorOut(w: PacketWriter, m: YouAreNotSpectatorOut) {
        w.int(m.int1)
    }
}
