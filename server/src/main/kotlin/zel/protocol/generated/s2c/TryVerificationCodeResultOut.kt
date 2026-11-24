package zel.protocol.generated

data class TryVerificationCodeResultOut(
    val int1: Int,
    val int2: Int,
)

object TryVerificationCodeResultOutCodec : OutgoingCodec<TryVerificationCodeResultOut> {
    override val name = "TryVerificationCodeResultOut"

    override fun read(r: PacketReader): TryVerificationCodeResultOut = readTryVerificationCodeResultOut(r)

    override fun write(w: PacketWriter, m: TryVerificationCodeResultOut) = writeTryVerificationCodeResultOut(w, m)

    private fun readTryVerificationCodeResultOut(r: PacketReader): TryVerificationCodeResultOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return TryVerificationCodeResultOut(vInt1, vInt2)
    }

    private fun writeTryVerificationCodeResultOut(w: PacketWriter, m: TryVerificationCodeResultOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
