package zel.protocol.generated

data class CameraSnapshotOut(
    val string1: String,
    val int1: Int,
)

object CameraSnapshotOutCodec : OutgoingCodec<CameraSnapshotOut> {
    override val name = "CameraSnapshotOut"

    override fun read(r: PacketReader): CameraSnapshotOut = readCameraSnapshotOut(r)

    override fun write(w: PacketWriter, m: CameraSnapshotOut) = writeCameraSnapshotOut(w, m)

    private fun readCameraSnapshotOut(r: PacketReader): CameraSnapshotOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        return CameraSnapshotOut(vString1, vInt1)
    }

    private fun writeCameraSnapshotOut(w: PacketWriter, m: CameraSnapshotOut) {
        w.string(m.string1)
        w.int(m.int1)
    }
}
