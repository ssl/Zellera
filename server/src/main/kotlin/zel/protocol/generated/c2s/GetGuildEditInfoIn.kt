package zel.protocol.generated

data class GetGuildEditInfoIn(
    val guildId: Int,
)

object GetGuildEditInfoInCodec : IncomingCodec<GetGuildEditInfoIn> {
    override val name = "GetGuildEditInfoIn"

    override fun read(r: PacketReader): GetGuildEditInfoIn = readGetGuildEditInfoIn(r)

    override fun write(w: PacketWriter, m: GetGuildEditInfoIn) = writeGetGuildEditInfoIn(w, m)

    private fun readGetGuildEditInfoIn(r: PacketReader): GetGuildEditInfoIn {
        val vGuildId = r.int()
        return GetGuildEditInfoIn(vGuildId)
    }

    private fun writeGetGuildEditInfoIn(w: PacketWriter, m: GetGuildEditInfoIn) {
        w.int(m.guildId)
    }
}
