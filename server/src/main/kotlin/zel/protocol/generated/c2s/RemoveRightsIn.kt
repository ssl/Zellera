package zel.protocol.generated

data class RemoveRightsIn(
    val userIds: List<Int>,
)

object RemoveRightsInCodec : IncomingCodec<RemoveRightsIn> {
    override val name = "RemoveRightsIn"

    override fun read(r: PacketReader): RemoveRightsIn = readRemoveRightsIn(r)

    override fun write(w: PacketWriter, m: RemoveRightsIn) = writeRemoveRightsIn(w, m)

    private fun readRemoveRightsIn(r: PacketReader): RemoveRightsIn {
        val vUserIds = List(r.countInt(4)) { r.int() }
        return RemoveRightsIn(vUserIds)
    }

    private fun writeRemoveRightsIn(w: PacketWriter, m: RemoveRightsIn) {
        w.countInt(m.userIds.size)
        m.userIds.forEach { w.int(it) }
    }
}
