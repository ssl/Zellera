package zel.protocol.generated

data class CreditFurniRedeemIn(
    val itemId: Int,
)

object CreditFurniRedeemInCodec : IncomingCodec<CreditFurniRedeemIn> {
    override val name = "CreditFurniRedeemIn"

    override fun read(r: PacketReader): CreditFurniRedeemIn = readCreditFurniRedeemIn(r)

    override fun write(w: PacketWriter, m: CreditFurniRedeemIn) = writeCreditFurniRedeemIn(w, m)

    private fun readCreditFurniRedeemIn(r: PacketReader): CreditFurniRedeemIn {
        val vItemId = r.int()
        return CreditFurniRedeemIn(vItemId)
    }

    private fun writeCreditFurniRedeemIn(w: PacketWriter, m: CreditFurniRedeemIn) {
        w.int(m.itemId)
    }
}
