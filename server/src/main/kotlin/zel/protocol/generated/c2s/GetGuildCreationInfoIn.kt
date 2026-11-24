package zel.protocol.generated

data object GetGuildCreationInfoIn

object GetGuildCreationInfoInCodec : IncomingCodec<GetGuildCreationInfoIn> {
    override val name = "GetGuildCreationInfoIn"

    override fun read(r: PacketReader): GetGuildCreationInfoIn = GetGuildCreationInfoIn

    override fun write(w: PacketWriter, m: GetGuildCreationInfoIn) = writeGetGuildCreationInfoIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetGuildCreationInfoIn(w: PacketWriter, m: GetGuildCreationInfoIn) {
    }
}
