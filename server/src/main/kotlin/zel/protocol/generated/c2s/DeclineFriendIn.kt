package zel.protocol.generated

data class DeclineFriendIn(
    val declineAll: Boolean,
    val userIds: List<Int>,
)

object DeclineFriendInCodec : IncomingCodec<DeclineFriendIn> {
    override val name = "DeclineFriendIn"

    override fun read(r: PacketReader): DeclineFriendIn = readDeclineFriendIn(r)

    override fun write(w: PacketWriter, m: DeclineFriendIn) = writeDeclineFriendIn(w, m)

    private fun readDeclineFriendIn(r: PacketReader): DeclineFriendIn {
        val vDeclineAll = r.boolean()
        val vUserIds = List(r.countInt(4)) { r.int() }
        return DeclineFriendIn(vDeclineAll, vUserIds)
    }

    private fun writeDeclineFriendIn(w: PacketWriter, m: DeclineFriendIn) {
        w.boolean(m.declineAll)
        w.countInt(m.userIds.size)
        m.userIds.forEach { w.int(it) }
    }
}
