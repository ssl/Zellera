package zel.protocol.generated

data class FriendFurniConfirmLockIn(
    val itemId: Int,
    val confirmed: Boolean,
)

object FriendFurniConfirmLockInCodec : IncomingCodec<FriendFurniConfirmLockIn> {
    override val name = "FriendFurniConfirmLockIn"

    override fun read(r: PacketReader): FriendFurniConfirmLockIn = readFriendFurniConfirmLockIn(r)

    override fun write(w: PacketWriter, m: FriendFurniConfirmLockIn) = writeFriendFurniConfirmLockIn(w, m)

    private fun readFriendFurniConfirmLockIn(r: PacketReader): FriendFurniConfirmLockIn {
        val vItemId = r.int()
        val vConfirmed = r.boolean()
        return FriendFurniConfirmLockIn(vItemId, vConfirmed)
    }

    private fun writeFriendFurniConfirmLockIn(w: PacketWriter, m: FriendFurniConfirmLockIn) {
        w.int(m.itemId)
        w.boolean(m.confirmed)
    }
}
