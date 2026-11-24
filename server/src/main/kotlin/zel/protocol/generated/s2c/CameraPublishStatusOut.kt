package zel.protocol.generated

data class CameraPublishStatusOut(
    val boolean1: Boolean,
    val int1: Int,
    val trailing1: String?,
)

object CameraPublishStatusOutCodec : OutgoingCodec<CameraPublishStatusOut> {
    override val name = "CameraPublishStatusOut"

    override fun read(r: PacketReader): CameraPublishStatusOut = readCameraPublishStatusOut(r)

    override fun write(w: PacketWriter, m: CameraPublishStatusOut) = writeCameraPublishStatusOut(w, m)

    private fun readCameraPublishStatusOut(r: PacketReader): CameraPublishStatusOut {
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vTrailing1 = if (r.remaining > 0) r.string() else null
        return CameraPublishStatusOut(vBoolean1, vInt1, vTrailing1)
    }

    private fun writeCameraPublishStatusOut(w: PacketWriter, m: CameraPublishStatusOut) {
        w.boolean(m.boolean1)
        w.int(m.int1)
        m.trailing1?.let { w.string(it) }
    }
}
