package zel.protocol.generated

data class TryPhoneNumberIn(
    val string1: String,
    val string2: String,
)

object TryPhoneNumberInCodec : IncomingCodec<TryPhoneNumberIn> {
    override val name = "TryPhoneNumberIn"

    override fun read(r: PacketReader): TryPhoneNumberIn = readTryPhoneNumberIn(r)

    override fun write(w: PacketWriter, m: TryPhoneNumberIn) = writeTryPhoneNumberIn(w, m)

    private fun readTryPhoneNumberIn(r: PacketReader): TryPhoneNumberIn {
        val vString1 = r.string()
        val vString2 = r.string()
        return TryPhoneNumberIn(vString1, vString2)
    }

    private fun writeTryPhoneNumberIn(w: PacketWriter, m: TryPhoneNumberIn) {
        w.string(m.string1)
        w.string(m.string2)
    }
}
