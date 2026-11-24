package zel.protocol.generated

data class ShowEnforceRoomCategoryDialogOut(
    val int1: Int,
)

object ShowEnforceRoomCategoryDialogOutCodec : OutgoingCodec<ShowEnforceRoomCategoryDialogOut> {
    override val name = "ShowEnforceRoomCategoryDialogOut"

    override fun read(r: PacketReader): ShowEnforceRoomCategoryDialogOut = readShowEnforceRoomCategoryDialogOut(r)

    override fun write(w: PacketWriter, m: ShowEnforceRoomCategoryDialogOut) = writeShowEnforceRoomCategoryDialogOut(w, m)

    private fun readShowEnforceRoomCategoryDialogOut(r: PacketReader): ShowEnforceRoomCategoryDialogOut {
        val vInt1 = r.int()
        return ShowEnforceRoomCategoryDialogOut(vInt1)
    }

    private fun writeShowEnforceRoomCategoryDialogOut(w: PacketWriter, m: ShowEnforceRoomCategoryDialogOut) {
        w.int(m.int1)
    }
}
