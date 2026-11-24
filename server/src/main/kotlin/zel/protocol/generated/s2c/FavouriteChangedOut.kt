package zel.protocol.generated

data class FavouriteChangedOut(
    val int1: Int,
    val boolean1: Boolean,
)

object FavouriteChangedOutCodec : OutgoingCodec<FavouriteChangedOut> {
    override val name = "FavouriteChangedOut"

    override fun read(r: PacketReader): FavouriteChangedOut = readFavouriteChangedOut(r)

    override fun write(w: PacketWriter, m: FavouriteChangedOut) = writeFavouriteChangedOut(w, m)

    private fun readFavouriteChangedOut(r: PacketReader): FavouriteChangedOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        return FavouriteChangedOut(vInt1, vBoolean1)
    }

    private fun writeFavouriteChangedOut(w: PacketWriter, m: FavouriteChangedOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
    }
}
