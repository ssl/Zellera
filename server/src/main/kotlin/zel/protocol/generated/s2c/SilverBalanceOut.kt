package zel.protocol.generated

data class SilverBalanceOut(
    val int1: Int,
)

object SilverBalanceOutCodec : OutgoingCodec<SilverBalanceOut> {
    override val name = "SilverBalanceOut"

    override fun read(r: PacketReader): SilverBalanceOut = readSilverBalanceOut(r)

    override fun write(w: PacketWriter, m: SilverBalanceOut) = writeSilverBalanceOut(w, m)

    private fun readSilverBalanceOut(r: PacketReader): SilverBalanceOut {
        val vInt1 = r.int()
        return SilverBalanceOut(vInt1)
    }

    private fun writeSilverBalanceOut(w: PacketWriter, m: SilverBalanceOut) {
        w.int(m.int1)
    }
}
