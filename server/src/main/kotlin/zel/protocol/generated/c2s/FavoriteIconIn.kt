package zel.protocol.generated

data class FavoriteIconIn(
    val id: Int,
)

object FavoriteIconInCodec : IncomingCodec<FavoriteIconIn> {
    override val name = "FavoriteIconIn"

    override fun read(r: PacketReader): FavoriteIconIn = readFavoriteIconIn(r)

    override fun write(w: PacketWriter, m: FavoriteIconIn) = writeFavoriteIconIn(w, m)

    private fun readFavoriteIconIn(r: PacketReader): FavoriteIconIn {
        val vId = r.int()
        return FavoriteIconIn(vId)
    }

    private fun writeFavoriteIconIn(w: PacketWriter, m: FavoriteIconIn) {
        w.int(m.id)
    }
}
