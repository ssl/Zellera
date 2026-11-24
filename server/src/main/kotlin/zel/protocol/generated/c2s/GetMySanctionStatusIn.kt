package zel.protocol.generated

data object GetMySanctionStatusIn

object GetMySanctionStatusInCodec : IncomingCodec<GetMySanctionStatusIn> {
    override val name = "GetMySanctionStatusIn"

    override fun read(r: PacketReader): GetMySanctionStatusIn = GetMySanctionStatusIn

    override fun write(w: PacketWriter, m: GetMySanctionStatusIn) = writeGetMySanctionStatusIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetMySanctionStatusIn(w: PacketWriter, m: GetMySanctionStatusIn) {
    }
}
