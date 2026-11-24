package zel.protocol.generated

data object GetRecyclerStatusIn

object GetRecyclerStatusInCodec : IncomingCodec<GetRecyclerStatusIn> {
    override val name = "GetRecyclerStatusIn"

    override fun read(r: PacketReader): GetRecyclerStatusIn = GetRecyclerStatusIn

    override fun write(w: PacketWriter, m: GetRecyclerStatusIn) = writeGetRecyclerStatusIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetRecyclerStatusIn(w: PacketWriter, m: GetRecyclerStatusIn) {
    }
}
