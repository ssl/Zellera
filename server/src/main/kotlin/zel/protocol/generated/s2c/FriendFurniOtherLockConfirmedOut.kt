package zel.protocol.generated

data class FriendFurniOtherLockConfirmedOut(
    val int1: Int,
)

object FriendFurniOtherLockConfirmedOutCodec : OutgoingCodec<FriendFurniOtherLockConfirmedOut> {
    override val name = "FriendFurniOtherLockConfirmedOut"

    override fun read(r: PacketReader): FriendFurniOtherLockConfirmedOut = readFriendFurniOtherLockConfirmedOut(r)

    override fun write(w: PacketWriter, m: FriendFurniOtherLockConfirmedOut) = writeFriendFurniOtherLockConfirmedOut(w, m)

    private fun readFriendFurniOtherLockConfirmedOut(r: PacketReader): FriendFurniOtherLockConfirmedOut {
        val vInt1 = r.int()
        return FriendFurniOtherLockConfirmedOut(vInt1)
    }

    private fun writeFriendFurniOtherLockConfirmedOut(w: PacketWriter, m: FriendFurniOtherLockConfirmedOut) {
        w.int(m.int1)
    }
}
