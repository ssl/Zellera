package zel.protocol.generated

data object GetRecyclerPrizesIn

object GetRecyclerPrizesInCodec : IncomingCodec<GetRecyclerPrizesIn> {
    override val name = "GetRecyclerPrizesIn"

    override fun read(r: PacketReader): GetRecyclerPrizesIn = GetRecyclerPrizesIn

    override fun write(w: PacketWriter, m: GetRecyclerPrizesIn) = writeGetRecyclerPrizesIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetRecyclerPrizesIn(w: PacketWriter, m: GetRecyclerPrizesIn) {
    }
}
