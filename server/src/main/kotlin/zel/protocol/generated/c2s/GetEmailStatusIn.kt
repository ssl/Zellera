package zel.protocol.generated

data object GetEmailStatusIn

object GetEmailStatusInCodec : IncomingCodec<GetEmailStatusIn> {
    override val name = "GetEmailStatusIn"

    override fun read(r: PacketReader): GetEmailStatusIn = GetEmailStatusIn

    override fun write(w: PacketWriter, m: GetEmailStatusIn) = writeGetEmailStatusIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetEmailStatusIn(w: PacketWriter, m: GetEmailStatusIn) {
    }
}
