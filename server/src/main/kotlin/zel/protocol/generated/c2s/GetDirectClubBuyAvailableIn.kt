package zel.protocol.generated

data class GetDirectClubBuyAvailableIn(
    val int1: Int,
)

object GetDirectClubBuyAvailableInCodec : IncomingCodec<GetDirectClubBuyAvailableIn> {
    override val name = "GetDirectClubBuyAvailableIn"

    override fun read(r: PacketReader): GetDirectClubBuyAvailableIn = readGetDirectClubBuyAvailableIn(r)

    override fun write(w: PacketWriter, m: GetDirectClubBuyAvailableIn) = writeGetDirectClubBuyAvailableIn(w, m)

    private fun readGetDirectClubBuyAvailableIn(r: PacketReader): GetDirectClubBuyAvailableIn {
        val vInt1 = r.int()
        return GetDirectClubBuyAvailableIn(vInt1)
    }

    private fun writeGetDirectClubBuyAvailableIn(w: PacketWriter, m: GetDirectClubBuyAvailableIn) {
        w.int(m.int1)
    }
}
