package zel.protocol.generated

data object GetNowPlayingIn

object GetNowPlayingInCodec : IncomingCodec<GetNowPlayingIn> {
    override val name = "GetNowPlayingIn"

    override fun read(r: PacketReader): GetNowPlayingIn = GetNowPlayingIn

    override fun write(w: PacketWriter, m: GetNowPlayingIn) = writeGetNowPlayingIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetNowPlayingIn(w: PacketWriter, m: GetNowPlayingIn) {
    }
}
