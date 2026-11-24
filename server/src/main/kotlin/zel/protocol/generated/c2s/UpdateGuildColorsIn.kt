package zel.protocol.generated

data class UpdateGuildColorsIn(
    val int1: Int,
    val int2: Int,
    val int3: Int,
)

object UpdateGuildColorsInCodec : IncomingCodec<UpdateGuildColorsIn> {
    override val name = "UpdateGuildColorsIn"

    override fun read(r: PacketReader): UpdateGuildColorsIn = readUpdateGuildColorsIn(r)

    override fun write(w: PacketWriter, m: UpdateGuildColorsIn) = writeUpdateGuildColorsIn(w, m)

    private fun readUpdateGuildColorsIn(r: PacketReader): UpdateGuildColorsIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return UpdateGuildColorsIn(vInt1, vInt2, vInt3)
    }

    private fun writeUpdateGuildColorsIn(w: PacketWriter, m: UpdateGuildColorsIn) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
