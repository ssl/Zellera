package zel.protocol.generated

data class GetRelationshipStatusInfoIn(
    val userId: Int,
)

object GetRelationshipStatusInfoInCodec : IncomingCodec<GetRelationshipStatusInfoIn> {
    override val name = "GetRelationshipStatusInfoIn"

    override fun read(r: PacketReader): GetRelationshipStatusInfoIn = readGetRelationshipStatusInfoIn(r)

    override fun write(w: PacketWriter, m: GetRelationshipStatusInfoIn) = writeGetRelationshipStatusInfoIn(w, m)

    private fun readGetRelationshipStatusInfoIn(r: PacketReader): GetRelationshipStatusInfoIn {
        val vUserId = r.int()
        return GetRelationshipStatusInfoIn(vUserId)
    }

    private fun writeGetRelationshipStatusInfoIn(w: PacketWriter, m: GetRelationshipStatusInfoIn) {
        w.int(m.userId)
    }
}
