package zel.protocol.generated

data class SelfDonateItemIn(
    val isWallItem: Boolean,
    val typeId: Int,
    val legacyPosterId: String,
    val amount: Int,
)

object SelfDonateItemInCodec : IncomingCodec<SelfDonateItemIn> {
    override val name = "SelfDonateItemIn"

    override fun read(r: PacketReader): SelfDonateItemIn = readSelfDonateItemIn(r)

    override fun write(w: PacketWriter, m: SelfDonateItemIn) = writeSelfDonateItemIn(w, m)

    private fun readSelfDonateItemIn(r: PacketReader): SelfDonateItemIn {
        val vIsWallItem = r.boolean()
        val vTypeId = r.int()
        val vLegacyPosterId = r.string()
        val vAmount = r.int()
        return SelfDonateItemIn(vIsWallItem, vTypeId, vLegacyPosterId, vAmount)
    }

    private fun writeSelfDonateItemIn(w: PacketWriter, m: SelfDonateItemIn) {
        w.boolean(m.isWallItem)
        w.int(m.typeId)
        w.string(m.legacyPosterId)
        w.int(m.amount)
    }
}
