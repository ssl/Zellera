package zel.protocol.generated

data class EmeraldBalanceOut(
    val int1: Int,
)

object EmeraldBalanceOutCodec : OutgoingCodec<EmeraldBalanceOut> {
    override val name = "EmeraldBalanceOut"

    override fun read(r: PacketReader): EmeraldBalanceOut = readEmeraldBalanceOut(r)

    override fun write(w: PacketWriter, m: EmeraldBalanceOut) = writeEmeraldBalanceOut(w, m)

    private fun readEmeraldBalanceOut(r: PacketReader): EmeraldBalanceOut {
        val vInt1 = r.int()
        return EmeraldBalanceOut(vInt1)
    }

    private fun writeEmeraldBalanceOut(w: PacketWriter, m: EmeraldBalanceOut) {
        w.int(m.int1)
    }
}
