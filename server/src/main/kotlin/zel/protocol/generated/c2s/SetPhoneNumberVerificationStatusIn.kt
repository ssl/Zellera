package zel.protocol.generated

data class SetPhoneNumberVerificationStatusIn(
    val int1: Int,
)

object SetPhoneNumberVerificationStatusInCodec : IncomingCodec<SetPhoneNumberVerificationStatusIn> {
    override val name = "SetPhoneNumberVerificationStatusIn"

    override fun read(r: PacketReader): SetPhoneNumberVerificationStatusIn = readSetPhoneNumberVerificationStatusIn(r)

    override fun write(w: PacketWriter, m: SetPhoneNumberVerificationStatusIn) = writeSetPhoneNumberVerificationStatusIn(w, m)

    private fun readSetPhoneNumberVerificationStatusIn(r: PacketReader): SetPhoneNumberVerificationStatusIn {
        val vInt1 = r.int()
        return SetPhoneNumberVerificationStatusIn(vInt1)
    }

    private fun writeSetPhoneNumberVerificationStatusIn(w: PacketWriter, m: SetPhoneNumberVerificationStatusIn) {
        w.int(m.int1)
    }
}
