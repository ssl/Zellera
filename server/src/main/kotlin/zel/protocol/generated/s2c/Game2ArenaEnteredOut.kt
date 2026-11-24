package zel.protocol.generated

data class Game2ArenaEnteredOut(
    val int1: Int,
    val string1: String,
    val string2: String,
    val string3: String,
    val int2: Int,
)

object Game2ArenaEnteredOutCodec : OutgoingCodec<Game2ArenaEnteredOut> {
    override val name = "Game2ArenaEnteredOut"

    override fun read(r: PacketReader): Game2ArenaEnteredOut = readGame2ArenaEnteredOut(r)

    override fun write(w: PacketWriter, m: Game2ArenaEnteredOut) = writeGame2ArenaEnteredOut(w, m)

    private fun readGame2ArenaEnteredOut(r: PacketReader): Game2ArenaEnteredOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt2 = r.int()
        return Game2ArenaEnteredOut(vInt1, vString1, vString2, vString3, vInt2)
    }

    private fun writeGame2ArenaEnteredOut(w: PacketWriter, m: Game2ArenaEnteredOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int2)
    }
}
