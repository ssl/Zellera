package zel.protocol.generated

data object GetGuildMembershipsIn

object GetGuildMembershipsInCodec : IncomingCodec<GetGuildMembershipsIn> {
    override val name = "GetGuildMembershipsIn"

    override fun read(r: PacketReader): GetGuildMembershipsIn = GetGuildMembershipsIn

    override fun write(w: PacketWriter, m: GetGuildMembershipsIn) = writeGetGuildMembershipsIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetGuildMembershipsIn(w: PacketWriter, m: GetGuildMembershipsIn) {
    }
}
