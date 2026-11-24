package zel.protocol.generated

data class Game2StartSnowballIn(
    val string1: String,
)

object Game2StartSnowballInCodec : IncomingCodec<Game2StartSnowballIn> {
    override val name = "Game2StartSnowballIn"

    override fun read(r: PacketReader): Game2StartSnowballIn = readGame2StartSnowballIn(r)

    override fun write(w: PacketWriter, m: Game2StartSnowballIn) = writeGame2StartSnowballIn(w, m)

    private fun readGame2StartSnowballIn(r: PacketReader): Game2StartSnowballIn {
        val vString1 = r.string()
        return Game2StartSnowballIn(vString1)
    }

    private fun writeGame2StartSnowballIn(w: PacketWriter, m: Game2StartSnowballIn) {
        w.string(m.string1)
    }
}
