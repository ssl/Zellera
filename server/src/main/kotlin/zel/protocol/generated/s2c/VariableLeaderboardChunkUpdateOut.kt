package zel.protocol.generated

data class VariableLeaderboardChunkUpdateOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
    val int3: Int,
    val int4: Int,
    val rest: ByteArray,
)

object VariableLeaderboardChunkUpdateOutCodec : OutgoingCodec<VariableLeaderboardChunkUpdateOut> {
    override val name = "VariableLeaderboardChunkUpdateOut"

    override fun read(r: PacketReader): VariableLeaderboardChunkUpdateOut = readVariableLeaderboardChunkUpdateOut(r)

    override fun write(w: PacketWriter, m: VariableLeaderboardChunkUpdateOut) = writeVariableLeaderboardChunkUpdateOut(w, m)

    private fun readVariableLeaderboardChunkUpdateOut(r: PacketReader): VariableLeaderboardChunkUpdateOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vRest = r.rest()
        return VariableLeaderboardChunkUpdateOut(vInt1, vInt2, vString1, vInt3, vInt4, vRest)
    }

    private fun writeVariableLeaderboardChunkUpdateOut(w: PacketWriter, m: VariableLeaderboardChunkUpdateOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
        w.int(m.int4)
        w.bytes(m.rest)
    }
}
