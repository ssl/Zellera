package zel.protocol.generated

data class SavePlayerNftWardrobeIn(
    val string1: String,
)

object SavePlayerNftWardrobeInCodec : IncomingCodec<SavePlayerNftWardrobeIn> {
    override val name = "SavePlayerNftWardrobeIn"

    override fun read(r: PacketReader): SavePlayerNftWardrobeIn = readSavePlayerNftWardrobeIn(r)

    override fun write(w: PacketWriter, m: SavePlayerNftWardrobeIn) = writeSavePlayerNftWardrobeIn(w, m)

    private fun readSavePlayerNftWardrobeIn(r: PacketReader): SavePlayerNftWardrobeIn {
        val vString1 = r.string()
        return SavePlayerNftWardrobeIn(vString1)
    }

    private fun writeSavePlayerNftWardrobeIn(w: PacketWriter, m: SavePlayerNftWardrobeIn) {
        w.string(m.string1)
    }
}
