package zel.protocol.generated

data class FriendFurniStartConfirmationOut(
    val int1: Int,
    val boolean1: Boolean,
)

object FriendFurniStartConfirmationOutCodec : OutgoingCodec<FriendFurniStartConfirmationOut> {
    override val name = "FriendFurniStartConfirmationOut"

    override fun read(r: PacketReader): FriendFurniStartConfirmationOut = readFriendFurniStartConfirmationOut(r)

    override fun write(w: PacketWriter, m: FriendFurniStartConfirmationOut) = writeFriendFurniStartConfirmationOut(w, m)

    private fun readFriendFurniStartConfirmationOut(r: PacketReader): FriendFurniStartConfirmationOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        return FriendFurniStartConfirmationOut(vInt1, vBoolean1)
    }

    private fun writeFriendFurniStartConfirmationOut(w: PacketWriter, m: FriendFurniStartConfirmationOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
    }
}
