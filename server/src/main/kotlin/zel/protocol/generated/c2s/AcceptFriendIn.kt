package zel.protocol.generated

data class AcceptFriendIn(
    val userIds: List<Int>,
)

object AcceptFriendInCodec : IncomingCodec<AcceptFriendIn> {
    override val name = "AcceptFriendIn"

    override fun read(r: PacketReader): AcceptFriendIn = readAcceptFriendIn(r)

    override fun write(w: PacketWriter, m: AcceptFriendIn) = writeAcceptFriendIn(w, m)

    private fun readAcceptFriendIn(r: PacketReader): AcceptFriendIn {
        val vUserIds = List(r.countInt(4)) { r.int() }
        return AcceptFriendIn(vUserIds)
    }

    private fun writeAcceptFriendIn(w: PacketWriter, m: AcceptFriendIn) {
        w.countInt(m.userIds.size)
        m.userIds.forEach { w.int(it) }
    }
}
