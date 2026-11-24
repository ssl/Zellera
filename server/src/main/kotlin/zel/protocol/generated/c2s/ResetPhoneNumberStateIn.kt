package zel.protocol.generated

data object ResetPhoneNumberStateIn

object ResetPhoneNumberStateInCodec : IncomingCodec<ResetPhoneNumberStateIn> {
    override val name = "ResetPhoneNumberStateIn"

    override fun read(r: PacketReader): ResetPhoneNumberStateIn = ResetPhoneNumberStateIn

    override fun write(w: PacketWriter, m: ResetPhoneNumberStateIn) = writeResetPhoneNumberStateIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeResetPhoneNumberStateIn(w: PacketWriter, m: ResetPhoneNumberStateIn) {
    }
}
