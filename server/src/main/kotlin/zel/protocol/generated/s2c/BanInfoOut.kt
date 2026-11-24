package zel.protocol.generated

data class BanInfoOut(
    val short1: Short,
    val string1: String,
    val int1: Int,
    val string2: String,
)

object BanInfoOutCodec : OutgoingCodec<BanInfoOut> {
    override val name = "BanInfoOut"

    override fun read(r: PacketReader): BanInfoOut = readBanInfoOut(r)

    override fun write(w: PacketWriter, m: BanInfoOut) = writeBanInfoOut(w, m)

    private fun readBanInfoOut(r: PacketReader): BanInfoOut {
        val vShort1 = r.short()
        val vString1 = r.string()
        val vInt1 = r.int()
        val vString2 = r.string()
        return BanInfoOut(vShort1, vString1, vInt1, vString2)
    }

    private fun writeBanInfoOut(w: PacketWriter, m: BanInfoOut) {
        w.short(m.short1)
        w.string(m.string1)
        w.int(m.int1)
        w.string(m.string2)
    }
}
