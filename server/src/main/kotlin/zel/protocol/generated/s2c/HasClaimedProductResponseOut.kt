package zel.protocol.generated

data class HasClaimedProductResponseOut(
    val string1: String,
    val boolean1: Boolean,
)

object HasClaimedProductResponseOutCodec : OutgoingCodec<HasClaimedProductResponseOut> {
    override val name = "HasClaimedProductResponseOut"

    override fun read(r: PacketReader): HasClaimedProductResponseOut = readHasClaimedProductResponseOut(r)

    override fun write(w: PacketWriter, m: HasClaimedProductResponseOut) = writeHasClaimedProductResponseOut(w, m)

    private fun readHasClaimedProductResponseOut(r: PacketReader): HasClaimedProductResponseOut {
        val vString1 = r.string()
        val vBoolean1 = r.boolean()
        return HasClaimedProductResponseOut(vString1, vBoolean1)
    }

    private fun writeHasClaimedProductResponseOut(w: PacketWriter, m: HasClaimedProductResponseOut) {
        w.string(m.string1)
        w.boolean(m.boolean1)
    }
}
