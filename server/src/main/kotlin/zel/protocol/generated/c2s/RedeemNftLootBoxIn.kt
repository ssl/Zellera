package zel.protocol.generated

data class RedeemNftLootBoxIn(
    val int1: Int,
)

object RedeemNftLootBoxInCodec : IncomingCodec<RedeemNftLootBoxIn> {
    override val name = "RedeemNftLootBoxIn"

    override fun read(r: PacketReader): RedeemNftLootBoxIn = readRedeemNftLootBoxIn(r)

    override fun write(w: PacketWriter, m: RedeemNftLootBoxIn) = writeRedeemNftLootBoxIn(w, m)

    private fun readRedeemNftLootBoxIn(r: PacketReader): RedeemNftLootBoxIn {
        val vInt1 = r.int()
        return RedeemNftLootBoxIn(vInt1)
    }

    private fun writeRedeemNftLootBoxIn(w: PacketWriter, m: RedeemNftLootBoxIn) {
        w.int(m.int1)
    }
}
