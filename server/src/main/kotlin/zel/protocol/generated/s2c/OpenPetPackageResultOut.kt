package zel.protocol.generated

data class OpenPetPackageResultOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
)

object OpenPetPackageResultOutCodec : OutgoingCodec<OpenPetPackageResultOut> {
    override val name = "OpenPetPackageResultOut"

    override fun read(r: PacketReader): OpenPetPackageResultOut = readOpenPetPackageResultOut(r)

    override fun write(w: PacketWriter, m: OpenPetPackageResultOut) = writeOpenPetPackageResultOut(w, m)

    private fun readOpenPetPackageResultOut(r: PacketReader): OpenPetPackageResultOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        return OpenPetPackageResultOut(vInt1, vInt2, vString1)
    }

    private fun writeOpenPetPackageResultOut(w: PacketWriter, m: OpenPetPackageResultOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
    }
}
