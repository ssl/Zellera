package zel.protocol.generated

data class YoutubeControlVideoOut(
    val int1: Int,
    val int2: Int,
)

object YoutubeControlVideoOutCodec : OutgoingCodec<YoutubeControlVideoOut> {
    override val name = "YoutubeControlVideoOut"

    override fun read(r: PacketReader): YoutubeControlVideoOut = readYoutubeControlVideoOut(r)

    override fun write(w: PacketWriter, m: YoutubeControlVideoOut) = writeYoutubeControlVideoOut(w, m)

    private fun readYoutubeControlVideoOut(r: PacketReader): YoutubeControlVideoOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return YoutubeControlVideoOut(vInt1, vInt2)
    }

    private fun writeYoutubeControlVideoOut(w: PacketWriter, m: YoutubeControlVideoOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
