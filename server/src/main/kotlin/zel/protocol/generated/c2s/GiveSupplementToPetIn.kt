package zel.protocol.generated

data class GiveSupplementToPetIn(
    val petId: Int,
    val supplementType: Int,
)

object GiveSupplementToPetInCodec : IncomingCodec<GiveSupplementToPetIn> {
    override val name = "GiveSupplementToPetIn"

    override fun read(r: PacketReader): GiveSupplementToPetIn = readGiveSupplementToPetIn(r)

    override fun write(w: PacketWriter, m: GiveSupplementToPetIn) = writeGiveSupplementToPetIn(w, m)

    private fun readGiveSupplementToPetIn(r: PacketReader): GiveSupplementToPetIn {
        val vPetId = r.int()
        val vSupplementType = r.int()
        return GiveSupplementToPetIn(vPetId, vSupplementType)
    }

    private fun writeGiveSupplementToPetIn(w: PacketWriter, m: GiveSupplementToPetIn) {
        w.int(m.petId)
        w.int(m.supplementType)
    }
}
