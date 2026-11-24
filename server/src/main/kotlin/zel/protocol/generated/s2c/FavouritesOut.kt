package zel.protocol.generated

data class FavouritesOut(
    val int1: Int,
    val list1: List<Int>,
)

object FavouritesOutCodec : OutgoingCodec<FavouritesOut> {
    override val name = "FavouritesOut"

    override fun read(r: PacketReader): FavouritesOut = readFavouritesOut(r)

    override fun write(w: PacketWriter, m: FavouritesOut) = writeFavouritesOut(w, m)

    private fun readFavouritesOut(r: PacketReader): FavouritesOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(4)) { r.int() }
        return FavouritesOut(vInt1, vList1)
    }

    private fun writeFavouritesOut(w: PacketWriter, m: FavouritesOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
    }
}
