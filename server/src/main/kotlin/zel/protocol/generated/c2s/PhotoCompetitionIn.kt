package zel.protocol.generated

data object PhotoCompetitionIn

object PhotoCompetitionInCodec : IncomingCodec<PhotoCompetitionIn> {
    override val name = "PhotoCompetitionIn"

    override fun read(r: PacketReader): PhotoCompetitionIn = PhotoCompetitionIn

    override fun write(w: PacketWriter, m: PhotoCompetitionIn) = writePhotoCompetitionIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writePhotoCompetitionIn(w: PacketWriter, m: PhotoCompetitionIn) {
    }
}
