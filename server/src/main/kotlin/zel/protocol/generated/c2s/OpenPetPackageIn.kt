package zel.protocol.generated

data class OpenPetPackageIn(
    val itemId: Int,
    val name: String,
)

object OpenPetPackageInCodec : IncomingCodec<OpenPetPackageIn> {
    override val name = "OpenPetPackageIn"

    override fun read(r: PacketReader): OpenPetPackageIn = readOpenPetPackageIn(r)

    override fun write(w: PacketWriter, m: OpenPetPackageIn) = writeOpenPetPackageIn(w, m)

    private fun readOpenPetPackageIn(r: PacketReader): OpenPetPackageIn {
        val vItemId = r.int()
        val vName = r.string()
        return OpenPetPackageIn(vItemId, vName)
    }

    private fun writeOpenPetPackageIn(w: PacketWriter, m: OpenPetPackageIn) {
        w.int(m.itemId)
        w.string(m.name)
    }
}
