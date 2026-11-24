package zel.protocol.generated

data class DeselectFavouriteGroupIn(
    val guildId: Int,
)

object DeselectFavouriteGroupInCodec : IncomingCodec<DeselectFavouriteGroupIn> {
    override val name = "DeselectFavouriteGroupIn"

    override fun read(r: PacketReader): DeselectFavouriteGroupIn = readDeselectFavouriteGroupIn(r)

    override fun write(w: PacketWriter, m: DeselectFavouriteGroupIn) = writeDeselectFavouriteGroupIn(w, m)

    private fun readDeselectFavouriteGroupIn(r: PacketReader): DeselectFavouriteGroupIn {
        val vGuildId = r.int()
        return DeselectFavouriteGroupIn(vGuildId)
    }

    private fun writeDeselectFavouriteGroupIn(w: PacketWriter, m: DeselectFavouriteGroupIn) {
        w.int(m.guildId)
    }
}
