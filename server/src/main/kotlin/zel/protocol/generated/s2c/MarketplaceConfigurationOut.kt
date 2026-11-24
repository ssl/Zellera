package zel.protocol.generated

data class MarketplaceConfigurationOut(
    val boolean1: Boolean,
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val int6: Int,
    val int7: Int,
    val int8: Int,
    val int9: Int,
    val int10: Int,
)

object MarketplaceConfigurationOutCodec : OutgoingCodec<MarketplaceConfigurationOut> {
    override val name = "MarketplaceConfigurationOut"

    override fun read(r: PacketReader): MarketplaceConfigurationOut = readMarketplaceConfigurationOut(r)

    override fun write(w: PacketWriter, m: MarketplaceConfigurationOut) = writeMarketplaceConfigurationOut(w, m)

    private fun readMarketplaceConfigurationOut(r: PacketReader): MarketplaceConfigurationOut {
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vInt9 = r.int()
        val vInt10 = r.int()
        return MarketplaceConfigurationOut(vBoolean1, vInt1, vInt2, vInt3, vInt4, vInt5, vInt6, vInt7, vInt8, vInt9, vInt10)
    }

    private fun writeMarketplaceConfigurationOut(w: PacketWriter, m: MarketplaceConfigurationOut) {
        w.boolean(m.boolean1)
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.int(m.int8)
        w.int(m.int9)
        w.int(m.int10)
    }
}
