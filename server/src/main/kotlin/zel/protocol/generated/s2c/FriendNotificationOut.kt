package zel.protocol.generated

data class FriendNotificationOut(
    val string1: String,
    val int1: Int,
    val string2: String,
)

object FriendNotificationOutCodec : OutgoingCodec<FriendNotificationOut> {
    override val name = "FriendNotificationOut"

    override fun read(r: PacketReader): FriendNotificationOut = readFriendNotificationOut(r)

    override fun write(w: PacketWriter, m: FriendNotificationOut) = writeFriendNotificationOut(w, m)

    private fun readFriendNotificationOut(r: PacketReader): FriendNotificationOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vString2 = r.string()
        return FriendNotificationOut(vString1, vInt1, vString2)
    }

    private fun writeFriendNotificationOut(w: PacketWriter, m: FriendNotificationOut) {
        w.string(m.string1)
        w.int(m.int1)
        w.string(m.string2)
    }
}
