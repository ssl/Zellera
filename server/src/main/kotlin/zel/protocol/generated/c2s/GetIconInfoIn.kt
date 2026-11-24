package zel.protocol.generated

data class GetIconInfoIn(
    val id: Int,
)

object GetIconInfoInCodec : IncomingCodec<GetIconInfoIn> {
    override val name = "GetIconInfoIn"

    override fun read(r: PacketReader): GetIconInfoIn = readGetIconInfoIn(r)

    override fun write(w: PacketWriter, m: GetIconInfoIn) = writeGetIconInfoIn(w, m)

    private fun readGetIconInfoIn(r: PacketReader): GetIconInfoIn {
        val vId = r.int()
        return GetIconInfoIn(vId)
    }

    private fun writeGetIconInfoIn(w: PacketWriter, m: GetIconInfoIn) {
        w.int(m.id)
    }
}
