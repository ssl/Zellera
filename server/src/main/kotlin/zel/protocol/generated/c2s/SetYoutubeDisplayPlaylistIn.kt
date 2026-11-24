package zel.protocol.generated

data class SetYoutubeDisplayPlaylistIn(
    val itemId: Int,
    val playlistId: String,
)

object SetYoutubeDisplayPlaylistInCodec : IncomingCodec<SetYoutubeDisplayPlaylistIn> {
    override val name = "SetYoutubeDisplayPlaylistIn"

    override fun read(r: PacketReader): SetYoutubeDisplayPlaylistIn = readSetYoutubeDisplayPlaylistIn(r)

    override fun write(w: PacketWriter, m: SetYoutubeDisplayPlaylistIn) = writeSetYoutubeDisplayPlaylistIn(w, m)

    private fun readSetYoutubeDisplayPlaylistIn(r: PacketReader): SetYoutubeDisplayPlaylistIn {
        val vItemId = r.int()
        val vPlaylistId = r.string()
        return SetYoutubeDisplayPlaylistIn(vItemId, vPlaylistId)
    }

    private fun writeSetYoutubeDisplayPlaylistIn(w: PacketWriter, m: SetYoutubeDisplayPlaylistIn) {
        w.int(m.itemId)
        w.string(m.playlistId)
    }
}
