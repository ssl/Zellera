package zel.protocol.generated

data class GetExtendedProfileIn(
    val playerId: Int,
    val openProfile: Boolean,
)

object GetExtendedProfileInCodec : IncomingCodec<GetExtendedProfileIn> {
    override val name = "GetExtendedProfileIn"

    override fun read(r: PacketReader): GetExtendedProfileIn = readGetExtendedProfileIn(r)

    override fun write(w: PacketWriter, m: GetExtendedProfileIn) = writeGetExtendedProfileIn(w, m)

    private fun readGetExtendedProfileIn(r: PacketReader): GetExtendedProfileIn {
        val vPlayerId = r.int()
        val vOpenProfile = r.boolean()
        return GetExtendedProfileIn(vPlayerId, vOpenProfile)
    }

    private fun writeGetExtendedProfileIn(w: PacketWriter, m: GetExtendedProfileIn) {
        w.int(m.playerId)
        w.boolean(m.openProfile)
    }
}
