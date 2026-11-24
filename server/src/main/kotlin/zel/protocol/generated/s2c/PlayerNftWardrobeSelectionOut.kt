package zel.protocol.generated

data class PlayerNftWardrobeSelectionOut(
    val string1: String,
    val string2: String,
    val string3: String,
)

object PlayerNftWardrobeSelectionOutCodec : OutgoingCodec<PlayerNftWardrobeSelectionOut> {
    override val name = "PlayerNftWardrobeSelectionOut"

    override fun read(r: PacketReader): PlayerNftWardrobeSelectionOut = readPlayerNftWardrobeSelectionOut(r)

    override fun write(w: PacketWriter, m: PlayerNftWardrobeSelectionOut) = writePlayerNftWardrobeSelectionOut(w, m)

    private fun readPlayerNftWardrobeSelectionOut(r: PacketReader): PlayerNftWardrobeSelectionOut {
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        return PlayerNftWardrobeSelectionOut(vString1, vString2, vString3)
    }

    private fun writePlayerNftWardrobeSelectionOut(w: PacketWriter, m: PlayerNftWardrobeSelectionOut) {
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
    }
}
