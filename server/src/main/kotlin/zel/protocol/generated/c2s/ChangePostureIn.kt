package zel.protocol.generated

data class ChangePostureIn(
    val posture: Int,
)

object ChangePostureInCodec : IncomingCodec<ChangePostureIn> {
    override val name = "ChangePostureIn"

    override fun read(r: PacketReader): ChangePostureIn = readChangePostureIn(r)

    override fun write(w: PacketWriter, m: ChangePostureIn) = writeChangePostureIn(w, m)

    private fun readChangePostureIn(r: PacketReader): ChangePostureIn {
        val vPosture = r.int()
        return ChangePostureIn(vPosture)
    }

    private fun writeChangePostureIn(w: PacketWriter, m: ChangePostureIn) {
        w.int(m.posture)
    }
}
