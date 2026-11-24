package zel.protocol.generated

data class TryPhoneNumberResultOut(
    val int1: Int,
    val int2: Int,
)

object TryPhoneNumberResultOutCodec : OutgoingCodec<TryPhoneNumberResultOut> {
    override val name = "TryPhoneNumberResultOut"

    override fun read(r: PacketReader): TryPhoneNumberResultOut = readTryPhoneNumberResultOut(r)

    override fun write(w: PacketWriter, m: TryPhoneNumberResultOut) = writeTryPhoneNumberResultOut(w, m)

    private fun readTryPhoneNumberResultOut(r: PacketReader): TryPhoneNumberResultOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return TryPhoneNumberResultOut(vInt1, vInt2)
    }

    private fun writeTryPhoneNumberResultOut(w: PacketWriter, m: TryPhoneNumberResultOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
