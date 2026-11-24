package zel.protocol.generated

data class InitCameraOut(
    val int1: Int,
    val int2: Int,
    val trailing1: Int?,
)

object InitCameraOutCodec : OutgoingCodec<InitCameraOut> {
    override val name = "InitCameraOut"

    override fun read(r: PacketReader): InitCameraOut = readInitCameraOut(r)

    override fun write(w: PacketWriter, m: InitCameraOut) = writeInitCameraOut(w, m)

    private fun readInitCameraOut(r: PacketReader): InitCameraOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vTrailing1 = if (r.remaining > 0) r.int() else null
        return InitCameraOut(vInt1, vInt2, vTrailing1)
    }

    private fun writeInitCameraOut(w: PacketWriter, m: InitCameraOut) {
        w.int(m.int1)
        w.int(m.int2)
        m.trailing1?.let { w.int(it) }
    }
}
