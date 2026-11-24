package zel.protocol.generated

data class YouArePlayingGameOut(
    val boolean1: Boolean,
)

object YouArePlayingGameOutCodec : OutgoingCodec<YouArePlayingGameOut> {
    override val name = "YouArePlayingGameOut"

    override fun read(r: PacketReader): YouArePlayingGameOut = readYouArePlayingGameOut(r)

    override fun write(w: PacketWriter, m: YouArePlayingGameOut) = writeYouArePlayingGameOut(w, m)

    private fun readYouArePlayingGameOut(r: PacketReader): YouArePlayingGameOut {
        val vBoolean1 = r.boolean()
        return YouArePlayingGameOut(vBoolean1)
    }

    private fun writeYouArePlayingGameOut(w: PacketWriter, m: YouArePlayingGameOut) {
        w.boolean(m.boolean1)
    }
}
