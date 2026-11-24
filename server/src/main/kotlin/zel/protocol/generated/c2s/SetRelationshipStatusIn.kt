package zel.protocol.generated

data class SetRelationshipStatusIn(
    val userId: Int,
    val relationId: Int,
)

object SetRelationshipStatusInCodec : IncomingCodec<SetRelationshipStatusIn> {
    override val name = "SetRelationshipStatusIn"

    override fun read(r: PacketReader): SetRelationshipStatusIn = readSetRelationshipStatusIn(r)

    override fun write(w: PacketWriter, m: SetRelationshipStatusIn) = writeSetRelationshipStatusIn(w, m)

    private fun readSetRelationshipStatusIn(r: PacketReader): SetRelationshipStatusIn {
        val vUserId = r.int()
        val vRelationId = r.int()
        return SetRelationshipStatusIn(vUserId, vRelationId)
    }

    private fun writeSetRelationshipStatusIn(w: PacketWriter, m: SetRelationshipStatusIn) {
        w.int(m.userId)
        w.int(m.relationId)
    }
}
