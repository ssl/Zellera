package zel.protocol.generated

data class GetYoutubeDisplayStatusIn(
    val itemId: Int,
)

object GetYoutubeDisplayStatusInCodec : IncomingCodec<GetYoutubeDisplayStatusIn> {
    override val name = "GetYoutubeDisplayStatusIn"

    override fun read(r: PacketReader): GetYoutubeDisplayStatusIn = readGetYoutubeDisplayStatusIn(r)

    override fun write(w: PacketWriter, m: GetYoutubeDisplayStatusIn) = writeGetYoutubeDisplayStatusIn(w, m)

    private fun readGetYoutubeDisplayStatusIn(r: PacketReader): GetYoutubeDisplayStatusIn {
        val vItemId = r.int()
        return GetYoutubeDisplayStatusIn(vItemId)
    }

    private fun writeGetYoutubeDisplayStatusIn(w: PacketWriter, m: GetYoutubeDisplayStatusIn) {
        w.int(m.itemId)
    }
}
