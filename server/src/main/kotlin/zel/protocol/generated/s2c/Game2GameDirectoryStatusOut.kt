package zel.protocol.generated

data class Game2GameDirectoryStatusOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val trailing1: Boolean?,
    val trailing2: Boolean?,
)

object Game2GameDirectoryStatusOutCodec : OutgoingCodec<Game2GameDirectoryStatusOut> {
    override val name = "Game2GameDirectoryStatusOut"

    override fun read(r: PacketReader): Game2GameDirectoryStatusOut = readGame2GameDirectoryStatusOut(r)

    override fun write(w: PacketWriter, m: Game2GameDirectoryStatusOut) = writeGame2GameDirectoryStatusOut(w, m)

    private fun readGame2GameDirectoryStatusOut(r: PacketReader): Game2GameDirectoryStatusOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vTrailing1 = if (r.remaining > 0) r.boolean() else null
        val vTrailing2 = if (r.remaining > 0) r.boolean() else null
        return Game2GameDirectoryStatusOut(vInt1, vInt2, vInt3, vInt4, vTrailing1, vTrailing2)
    }

    private fun writeGame2GameDirectoryStatusOut(w: PacketWriter, m: Game2GameDirectoryStatusOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        m.trailing1?.let { w.boolean(it) }
        m.trailing2?.let { w.boolean(it) }
    }
}
