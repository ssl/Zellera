package zel.protocol.generated

data class GetModeratorPlayerInfoIn(
    val int1: Int,
)

object GetModeratorPlayerInfoInCodec : IncomingCodec<GetModeratorPlayerInfoIn> {
    override val name = "GetModeratorPlayerInfoIn"

    override fun read(r: PacketReader): GetModeratorPlayerInfoIn = readGetModeratorPlayerInfoIn(r)

    override fun write(w: PacketWriter, m: GetModeratorPlayerInfoIn) = writeGetModeratorPlayerInfoIn(w, m)

    private fun readGetModeratorPlayerInfoIn(r: PacketReader): GetModeratorPlayerInfoIn {
        val vInt1 = r.int()
        return GetModeratorPlayerInfoIn(vInt1)
    }

    private fun writeGetModeratorPlayerInfoIn(w: PacketWriter, m: GetModeratorPlayerInfoIn) {
        w.int(m.int1)
    }
}
