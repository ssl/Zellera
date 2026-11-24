package zel.protocol.generated

data class NewFriendRequestOut(
    val int1: Int,
    val string1: String,
    val string2: String,
)

object NewFriendRequestOutCodec : OutgoingCodec<NewFriendRequestOut> {
    override val name = "NewFriendRequestOut"

    override fun read(r: PacketReader): NewFriendRequestOut = readNewFriendRequestOut(r)

    override fun write(w: PacketWriter, m: NewFriendRequestOut) = writeNewFriendRequestOut(w, m)

    private fun readNewFriendRequestOut(r: PacketReader): NewFriendRequestOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        return NewFriendRequestOut(vInt1, vString1, vString2)
    }

    private fun writeNewFriendRequestOut(w: PacketWriter, m: NewFriendRequestOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
    }
}
