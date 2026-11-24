package zel.protocol.generated

data class PlayerPurchasableChatStyleChangedOut(
    val boolean1: Boolean,
    val int1: Int,
)

object PlayerPurchasableChatStyleChangedOutCodec : OutgoingCodec<PlayerPurchasableChatStyleChangedOut> {
    override val name = "PlayerPurchasableChatStyleChangedOut"

    override fun read(r: PacketReader): PlayerPurchasableChatStyleChangedOut = readPlayerPurchasableChatStyleChangedOut(r)

    override fun write(w: PacketWriter, m: PlayerPurchasableChatStyleChangedOut) = writePlayerPurchasableChatStyleChangedOut(w, m)

    private fun readPlayerPurchasableChatStyleChangedOut(r: PacketReader): PlayerPurchasableChatStyleChangedOut {
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        return PlayerPurchasableChatStyleChangedOut(vBoolean1, vInt1)
    }

    private fun writePlayerPurchasableChatStyleChangedOut(w: PacketWriter, m: PlayerPurchasableChatStyleChangedOut) {
        w.boolean(m.boolean1)
        w.int(m.int1)
    }
}
