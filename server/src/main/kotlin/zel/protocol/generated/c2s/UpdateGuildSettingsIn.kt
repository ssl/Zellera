package zel.protocol.generated

data class UpdateGuildSettingsIn(
    val int1: Int,
    val int2: Int,
    val int3: Int,
)

object UpdateGuildSettingsInCodec : IncomingCodec<UpdateGuildSettingsIn> {
    override val name = "UpdateGuildSettingsIn"

    override fun read(r: PacketReader): UpdateGuildSettingsIn = readUpdateGuildSettingsIn(r)

    override fun write(w: PacketWriter, m: UpdateGuildSettingsIn) = writeUpdateGuildSettingsIn(w, m)

    private fun readUpdateGuildSettingsIn(r: PacketReader): UpdateGuildSettingsIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return UpdateGuildSettingsIn(vInt1, vInt2, vInt3)
    }

    private fun writeUpdateGuildSettingsIn(w: PacketWriter, m: UpdateGuildSettingsIn) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
