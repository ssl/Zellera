package zel.protocol.generated

data class RedeemNftLootBoxResultOut(
    val short1: Short,
)

object RedeemNftLootBoxResultOutCodec : OutgoingCodec<RedeemNftLootBoxResultOut> {
    override val name = "RedeemNftLootBoxResultOut"

    override fun read(r: PacketReader): RedeemNftLootBoxResultOut = readRedeemNftLootBoxResultOut(r)

    override fun write(w: PacketWriter, m: RedeemNftLootBoxResultOut) = writeRedeemNftLootBoxResultOut(w, m)

    private fun readRedeemNftLootBoxResultOut(r: PacketReader): RedeemNftLootBoxResultOut {
        val vShort1 = r.short()
        return RedeemNftLootBoxResultOut(vShort1)
    }

    private fun writeRedeemNftLootBoxResultOut(w: PacketWriter, m: RedeemNftLootBoxResultOut) {
        w.short(m.short1)
    }
}
