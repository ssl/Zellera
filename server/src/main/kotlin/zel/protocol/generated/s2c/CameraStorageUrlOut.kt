package zel.protocol.generated

data class CameraStorageUrlOut(
    val string1: String,
)

object CameraStorageUrlOutCodec : OutgoingCodec<CameraStorageUrlOut> {
    override val name = "CameraStorageUrlOut"

    override fun read(r: PacketReader): CameraStorageUrlOut = readCameraStorageUrlOut(r)

    override fun write(w: PacketWriter, m: CameraStorageUrlOut) = writeCameraStorageUrlOut(w, m)

    private fun readCameraStorageUrlOut(r: PacketReader): CameraStorageUrlOut {
        val vString1 = r.string()
        return CameraStorageUrlOut(vString1)
    }

    private fun writeCameraStorageUrlOut(w: PacketWriter, m: CameraStorageUrlOut) {
        w.string(m.string1)
    }
}
