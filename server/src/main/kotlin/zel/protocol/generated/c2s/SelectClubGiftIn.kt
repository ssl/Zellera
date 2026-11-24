package zel.protocol.generated

data class SelectClubGiftIn(
    val itemName: String,
)

object SelectClubGiftInCodec : IncomingCodec<SelectClubGiftIn> {
    override val name = "SelectClubGiftIn"

    override fun read(r: PacketReader): SelectClubGiftIn = readSelectClubGiftIn(r)

    override fun write(w: PacketWriter, m: SelectClubGiftIn) = writeSelectClubGiftIn(w, m)

    private fun readSelectClubGiftIn(r: PacketReader): SelectClubGiftIn {
        val vItemName = r.string()
        return SelectClubGiftIn(vItemName)
    }

    private fun writeSelectClubGiftIn(w: PacketWriter, m: SelectClubGiftIn) {
        w.string(m.itemName)
    }
}
