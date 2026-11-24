package zel.protocol.generated

data class GetGroupDetailsIn(
    val guildId: Int,
    val newWindow: Boolean,
)

object GetGroupDetailsInCodec : IncomingCodec<GetGroupDetailsIn> {
    override val name = "GetGroupDetailsIn"

    override fun read(r: PacketReader): GetGroupDetailsIn = readGetGroupDetailsIn(r)

    override fun write(w: PacketWriter, m: GetGroupDetailsIn) = writeGetGroupDetailsIn(w, m)

    private fun readGetGroupDetailsIn(r: PacketReader): GetGroupDetailsIn {
        val vGuildId = r.int()
        val vNewWindow = r.boolean()
        return GetGroupDetailsIn(vGuildId, vNewWindow)
    }

    private fun writeGetGroupDetailsIn(w: PacketWriter, m: GetGroupDetailsIn) {
        w.int(m.guildId)
        w.boolean(m.newWindow)
    }
}
