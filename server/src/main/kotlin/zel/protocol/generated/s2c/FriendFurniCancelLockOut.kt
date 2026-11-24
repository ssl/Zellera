package zel.protocol.generated

data class FriendFurniCancelLockOut(
    val int1: Int,
)

object FriendFurniCancelLockOutCodec : OutgoingCodec<FriendFurniCancelLockOut> {
    override val name = "FriendFurniCancelLockOut"

    override fun read(r: PacketReader): FriendFurniCancelLockOut = readFriendFurniCancelLockOut(r)

    override fun write(w: PacketWriter, m: FriendFurniCancelLockOut) = writeFriendFurniCancelLockOut(w, m)

    private fun readFriendFurniCancelLockOut(r: PacketReader): FriendFurniCancelLockOut {
        val vInt1 = r.int()
        return FriendFurniCancelLockOut(vInt1)
    }

    private fun writeFriendFurniCancelLockOut(w: PacketWriter, m: FriendFurniCancelLockOut) {
        w.int(m.int1)
    }
}
