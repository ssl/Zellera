package zel.protocol.generated

data object PublishPhotoIn

object PublishPhotoInCodec : IncomingCodec<PublishPhotoIn> {
    override val name = "PublishPhotoIn"

    override fun read(r: PacketReader): PublishPhotoIn = PublishPhotoIn

    override fun write(w: PacketWriter, m: PublishPhotoIn) = writePublishPhotoIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writePublishPhotoIn(w: PacketWriter, m: PublishPhotoIn) {
    }
}
