package zel.protocol.generated

data class GetClubOffersIn(
    val int1: Int,
)

object GetClubOffersInCodec : IncomingCodec<GetClubOffersIn> {
    override val name = "GetClubOffersIn"

    override fun read(r: PacketReader): GetClubOffersIn = readGetClubOffersIn(r)

    override fun write(w: PacketWriter, m: GetClubOffersIn) = writeGetClubOffersIn(w, m)

    private fun readGetClubOffersIn(r: PacketReader): GetClubOffersIn {
        val vInt1 = r.int()
        return GetClubOffersIn(vInt1)
    }

    private fun writeGetClubOffersIn(w: PacketWriter, m: GetClubOffersIn) {
        w.int(m.int1)
    }
}
