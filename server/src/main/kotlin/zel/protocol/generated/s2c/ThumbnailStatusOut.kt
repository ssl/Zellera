package zel.protocol.generated

data class ThumbnailStatusOut(
    val trailing1: Trailing1?,
) {
    data class Trailing1(
        val boolean1: Boolean,
        val boolean2: Boolean,
    )
}

object ThumbnailStatusOutCodec : OutgoingCodec<ThumbnailStatusOut> {
    override val name = "ThumbnailStatusOut"

    override fun read(r: PacketReader): ThumbnailStatusOut = readThumbnailStatusOut(r)

    override fun write(w: PacketWriter, m: ThumbnailStatusOut) = writeThumbnailStatusOut(w, m)

    private fun readThumbnailStatusOutTrailing1(r: PacketReader): ThumbnailStatusOut.Trailing1 {
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        return ThumbnailStatusOut.Trailing1(vBoolean1, vBoolean2)
    }

    private fun readThumbnailStatusOut(r: PacketReader): ThumbnailStatusOut {
        val vTrailing1 = if (r.remaining > 0) readThumbnailStatusOutTrailing1(r) else null
        return ThumbnailStatusOut(vTrailing1)
    }

    private fun writeThumbnailStatusOutTrailing1(w: PacketWriter, m: ThumbnailStatusOut.Trailing1) {
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
    }

    private fun writeThumbnailStatusOut(w: PacketWriter, m: ThumbnailStatusOut) {
        m.trailing1?.let { writeThumbnailStatusOutTrailing1(w, it) }
    }
}
