package zel.protocol.generated

data class SaveWardrobeOutfitIn(
    val slotId: Int,
    val look: String,
    val gender: String,
)

object SaveWardrobeOutfitInCodec : IncomingCodec<SaveWardrobeOutfitIn> {
    override val name = "SaveWardrobeOutfitIn"

    override fun read(r: PacketReader): SaveWardrobeOutfitIn = readSaveWardrobeOutfitIn(r)

    override fun write(w: PacketWriter, m: SaveWardrobeOutfitIn) = writeSaveWardrobeOutfitIn(w, m)

    private fun readSaveWardrobeOutfitIn(r: PacketReader): SaveWardrobeOutfitIn {
        val vSlotId = r.int()
        val vLook = r.string()
        val vGender = r.string()
        return SaveWardrobeOutfitIn(vSlotId, vLook, vGender)
    }

    private fun writeSaveWardrobeOutfitIn(w: PacketWriter, m: SaveWardrobeOutfitIn) {
        w.int(m.slotId)
        w.string(m.look)
        w.string(m.gender)
    }
}
