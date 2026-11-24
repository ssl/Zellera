package zel.protocol.generated

data class UpdateRoomCategoryAndTradeSettingsIn(
    val roomId: Int,
    val categoryId: Int,
    val tradeMode: Int,
)

object UpdateRoomCategoryAndTradeSettingsInCodec : IncomingCodec<UpdateRoomCategoryAndTradeSettingsIn> {
    override val name = "UpdateRoomCategoryAndTradeSettingsIn"

    override fun read(r: PacketReader): UpdateRoomCategoryAndTradeSettingsIn = readUpdateRoomCategoryAndTradeSettingsIn(r)

    override fun write(w: PacketWriter, m: UpdateRoomCategoryAndTradeSettingsIn) = writeUpdateRoomCategoryAndTradeSettingsIn(w, m)

    private fun readUpdateRoomCategoryAndTradeSettingsIn(r: PacketReader): UpdateRoomCategoryAndTradeSettingsIn {
        val vRoomId = r.int()
        val vCategoryId = r.int()
        val vTradeMode = r.int()
        return UpdateRoomCategoryAndTradeSettingsIn(vRoomId, vCategoryId, vTradeMode)
    }

    private fun writeUpdateRoomCategoryAndTradeSettingsIn(w: PacketWriter, m: UpdateRoomCategoryAndTradeSettingsIn) {
        w.int(m.roomId)
        w.int(m.categoryId)
        w.int(m.tradeMode)
    }
}
