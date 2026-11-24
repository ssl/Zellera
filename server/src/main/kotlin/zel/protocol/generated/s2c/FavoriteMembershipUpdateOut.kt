package zel.protocol.generated

data class FavoriteMembershipUpdateOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val string1: String,
)

object FavoriteMembershipUpdateOutCodec : OutgoingCodec<FavoriteMembershipUpdateOut> {
    override val name = "FavoriteMembershipUpdateOut"

    override fun read(r: PacketReader): FavoriteMembershipUpdateOut = readFavoriteMembershipUpdateOut(r)

    override fun write(w: PacketWriter, m: FavoriteMembershipUpdateOut) = writeFavoriteMembershipUpdateOut(w, m)

    private fun readFavoriteMembershipUpdateOut(r: PacketReader): FavoriteMembershipUpdateOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        return FavoriteMembershipUpdateOut(vInt1, vInt2, vInt3, vString1)
    }

    private fun writeFavoriteMembershipUpdateOut(w: PacketWriter, m: FavoriteMembershipUpdateOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
    }
}
