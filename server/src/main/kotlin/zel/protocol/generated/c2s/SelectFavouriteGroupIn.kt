package zel.protocol.generated

data class SelectFavouriteGroupIn(
    val guildId: Int,
)

object SelectFavouriteGroupInCodec : IncomingCodec<SelectFavouriteGroupIn> {
    override val name = "SelectFavouriteGroupIn"

    override fun read(r: PacketReader): SelectFavouriteGroupIn = readSelectFavouriteGroupIn(r)

    override fun write(w: PacketWriter, m: SelectFavouriteGroupIn) = writeSelectFavouriteGroupIn(w, m)

    private fun readSelectFavouriteGroupIn(r: PacketReader): SelectFavouriteGroupIn {
        val vGuildId = r.int()
        return SelectFavouriteGroupIn(vGuildId)
    }

    private fun writeSelectFavouriteGroupIn(w: PacketWriter, m: SelectFavouriteGroupIn) {
        w.int(m.guildId)
    }
}
