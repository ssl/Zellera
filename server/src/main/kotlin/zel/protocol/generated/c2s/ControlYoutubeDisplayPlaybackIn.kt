package zel.protocol.generated

data class ControlYoutubeDisplayPlaybackIn(
    val itemId: Int,
    val state: Int,
)

object ControlYoutubeDisplayPlaybackInCodec : IncomingCodec<ControlYoutubeDisplayPlaybackIn> {
    override val name = "ControlYoutubeDisplayPlaybackIn"

    override fun read(r: PacketReader): ControlYoutubeDisplayPlaybackIn = readControlYoutubeDisplayPlaybackIn(r)

    override fun write(w: PacketWriter, m: ControlYoutubeDisplayPlaybackIn) = writeControlYoutubeDisplayPlaybackIn(w, m)

    private fun readControlYoutubeDisplayPlaybackIn(r: PacketReader): ControlYoutubeDisplayPlaybackIn {
        val vItemId = r.int()
        val vState = r.int()
        return ControlYoutubeDisplayPlaybackIn(vItemId, vState)
    }

    private fun writeControlYoutubeDisplayPlaybackIn(w: PacketWriter, m: ControlYoutubeDisplayPlaybackIn) {
        w.int(m.itemId)
        w.int(m.state)
    }
}
