package zel.protocol.generated

data object RequestCameraConfigurationIn

object RequestCameraConfigurationInCodec : IncomingCodec<RequestCameraConfigurationIn> {
    override val name = "RequestCameraConfigurationIn"

    override fun read(r: PacketReader): RequestCameraConfigurationIn = RequestCameraConfigurationIn

    override fun write(w: PacketWriter, m: RequestCameraConfigurationIn) = writeRequestCameraConfigurationIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeRequestCameraConfigurationIn(w: PacketWriter, m: RequestCameraConfigurationIn) {
    }
}
