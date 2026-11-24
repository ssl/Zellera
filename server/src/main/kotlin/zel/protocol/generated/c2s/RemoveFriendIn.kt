package zel.protocol.generated

data class RemoveFriendIn(
    val userIds: List<Int>,
)

object RemoveFriendInCodec : IncomingCodec<RemoveFriendIn> {
    override val name = "RemoveFriendIn"

    override fun read(r: PacketReader): RemoveFriendIn = readRemoveFriendIn(r)

    override fun write(w: PacketWriter, m: RemoveFriendIn) = writeRemoveFriendIn(w, m)

    private fun readRemoveFriendIn(r: PacketReader): RemoveFriendIn {
        val vUserIds = List(r.countInt(4)) { r.int() }
        return RemoveFriendIn(vUserIds)
    }

    private fun writeRemoveFriendIn(w: PacketWriter, m: RemoveFriendIn) {
        w.countInt(m.userIds.size)
        m.userIds.forEach { w.int(it) }
    }
}
