package zel.protocol.generated

data object NewPlayerExperienceScriptProceedIn

object NewPlayerExperienceScriptProceedInCodec : IncomingCodec<NewPlayerExperienceScriptProceedIn> {
    override val name = "NewPlayerExperienceScriptProceedIn"

    override fun read(r: PacketReader): NewPlayerExperienceScriptProceedIn = NewPlayerExperienceScriptProceedIn

    override fun write(w: PacketWriter, m: NewPlayerExperienceScriptProceedIn) = writeNewPlayerExperienceScriptProceedIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeNewPlayerExperienceScriptProceedIn(w: PacketWriter, m: NewPlayerExperienceScriptProceedIn) {
    }
}
