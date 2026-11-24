package zel.protocol.generated

data object NewPlayerExperienceNotCompleteOut

object NewPlayerExperienceNotCompleteOutCodec : OutgoingCodec<NewPlayerExperienceNotCompleteOut> {
    override val name = "NewPlayerExperienceNotCompleteOut"

    override fun read(r: PacketReader): NewPlayerExperienceNotCompleteOut = NewPlayerExperienceNotCompleteOut

    override fun write(w: PacketWriter, m: NewPlayerExperienceNotCompleteOut) = writeNewPlayerExperienceNotCompleteOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeNewPlayerExperienceNotCompleteOut(w: PacketWriter, m: NewPlayerExperienceNotCompleteOut) {
    }
}
