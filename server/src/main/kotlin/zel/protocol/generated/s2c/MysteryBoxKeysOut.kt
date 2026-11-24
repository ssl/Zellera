package zel.protocol.generated

data class MysteryBoxKeysOut(
    val string1: String,
    val string2: String,
)

object MysteryBoxKeysOutCodec : OutgoingCodec<MysteryBoxKeysOut> {
    override val name = "MysteryBoxKeysOut"

    override fun read(r: PacketReader): MysteryBoxKeysOut = readMysteryBoxKeysOut(r)

    override fun write(w: PacketWriter, m: MysteryBoxKeysOut) = writeMysteryBoxKeysOut(w, m)

    private fun readMysteryBoxKeysOut(r: PacketReader): MysteryBoxKeysOut {
        val vString1 = r.string()
        val vString2 = r.string()
        return MysteryBoxKeysOut(vString1, vString2)
    }

    private fun writeMysteryBoxKeysOut(w: PacketWriter, m: MysteryBoxKeysOut) {
        w.string(m.string1)
        w.string(m.string2)
    }
}
