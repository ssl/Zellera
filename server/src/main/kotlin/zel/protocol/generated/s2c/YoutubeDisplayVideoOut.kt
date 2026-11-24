package zel.protocol.generated

data class YoutubeDisplayVideoOut(
    val int1: Int,
    val string1: String,
    val int2: Int,
    val int3: Int,
    val int4: Int,
)

object YoutubeDisplayVideoOutCodec : OutgoingCodec<YoutubeDisplayVideoOut> {
    override val name = "YoutubeDisplayVideoOut"

    override fun read(r: PacketReader): YoutubeDisplayVideoOut = readYoutubeDisplayVideoOut(r)

    override fun write(w: PacketWriter, m: YoutubeDisplayVideoOut) = writeYoutubeDisplayVideoOut(w, m)

    private fun readYoutubeDisplayVideoOut(r: PacketReader): YoutubeDisplayVideoOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        return YoutubeDisplayVideoOut(vInt1, vString1, vInt2, vInt3, vInt4)
    }

    private fun writeYoutubeDisplayVideoOut(w: PacketWriter, m: YoutubeDisplayVideoOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
    }
}
