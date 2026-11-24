package zel.protocol.generated

data class Game2PlayerJoinedGameOut(
    val int1: Int,
    val string1: String,
    val string2: String,
    val string3: String,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val boolean1: Boolean,
)

object Game2PlayerJoinedGameOutCodec : OutgoingCodec<Game2PlayerJoinedGameOut> {
    override val name = "Game2PlayerJoinedGameOut"

    override fun read(r: PacketReader): Game2PlayerJoinedGameOut = readGame2PlayerJoinedGameOut(r)

    override fun write(w: PacketWriter, m: Game2PlayerJoinedGameOut) = writeGame2PlayerJoinedGameOut(w, m)

    private fun readGame2PlayerJoinedGameOut(r: PacketReader): Game2PlayerJoinedGameOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vBoolean1 = r.boolean()
        return Game2PlayerJoinedGameOut(vInt1, vString1, vString2, vString3, vInt2, vInt3, vInt4, vInt5, vBoolean1)
    }

    private fun writeGame2PlayerJoinedGameOut(w: PacketWriter, m: Game2PlayerJoinedGameOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.boolean(m.boolean1)
    }
}
