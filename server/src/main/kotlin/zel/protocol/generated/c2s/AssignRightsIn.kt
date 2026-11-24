package zel.protocol.generated

data class AssignRightsIn(
    val userId: Int,
)

object AssignRightsInCodec : IncomingCodec<AssignRightsIn> {
    override val name = "AssignRightsIn"

    override fun read(r: PacketReader): AssignRightsIn = readAssignRightsIn(r)

    override fun write(w: PacketWriter, m: AssignRightsIn) = writeAssignRightsIn(w, m)

    private fun readAssignRightsIn(r: PacketReader): AssignRightsIn {
        val vUserId = r.int()
        return AssignRightsIn(vUserId)
    }

    private fun writeAssignRightsIn(w: PacketWriter, m: AssignRightsIn) {
        w.int(m.userId)
    }
}
