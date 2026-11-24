package zel.protocol.generated

data object GetBundleDiscountRulesetIn

object GetBundleDiscountRulesetInCodec : IncomingCodec<GetBundleDiscountRulesetIn> {
    override val name = "GetBundleDiscountRulesetIn"

    override fun read(r: PacketReader): GetBundleDiscountRulesetIn = GetBundleDiscountRulesetIn

    override fun write(w: PacketWriter, m: GetBundleDiscountRulesetIn) = writeGetBundleDiscountRulesetIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetBundleDiscountRulesetIn(w: PacketWriter, m: GetBundleDiscountRulesetIn) {
    }
}
