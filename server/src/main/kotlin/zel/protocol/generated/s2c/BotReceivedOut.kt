package zel.protocol.generated

data class BotReceivedOut(
    val boolean1: Boolean,
    val int1: Int,
    val string1: String,
    val string2: String,
    val string3: String,
    val string4: String,
)

object BotReceivedOutCodec : OutgoingCodec<BotReceivedOut> {
    override val name = "BotReceivedOut"

    override fun read(r: PacketReader): BotReceivedOut = readBotReceivedOut(r)

    override fun write(w: PacketWriter, m: BotReceivedOut) = writeBotReceivedOut(w, m)

    private fun readBotReceivedOut(r: PacketReader): BotReceivedOut {
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        val vString4 = r.string()
        return BotReceivedOut(vBoolean1, vInt1, vString1, vString2, vString3, vString4)
    }

    private fun writeBotReceivedOut(w: PacketWriter, m: BotReceivedOut) {
        w.boolean(m.boolean1)
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
        w.string(m.string4)
    }
}
