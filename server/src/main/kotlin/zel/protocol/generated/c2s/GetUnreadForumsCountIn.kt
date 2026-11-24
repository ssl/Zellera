package zel.protocol.generated

data object GetUnreadForumsCountIn

object GetUnreadForumsCountInCodec : IncomingCodec<GetUnreadForumsCountIn> {
    override val name = "GetUnreadForumsCountIn"

    override fun read(r: PacketReader): GetUnreadForumsCountIn = GetUnreadForumsCountIn

    override fun write(w: PacketWriter, m: GetUnreadForumsCountIn) = writeGetUnreadForumsCountIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetUnreadForumsCountIn(w: PacketWriter, m: GetUnreadForumsCountIn) {
    }
}
