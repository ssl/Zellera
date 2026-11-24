package zel.protocol.generated

data class SetSoundSettingsIn(
    val system: Int,
    val furni: Int,
    val trax: Int,
)

object SetSoundSettingsInCodec : IncomingCodec<SetSoundSettingsIn> {
    override val name = "SetSoundSettingsIn"

    override fun read(r: PacketReader): SetSoundSettingsIn = readSetSoundSettingsIn(r)

    override fun write(w: PacketWriter, m: SetSoundSettingsIn) = writeSetSoundSettingsIn(w, m)

    private fun readSetSoundSettingsIn(r: PacketReader): SetSoundSettingsIn {
        val vSystem = r.int()
        val vFurni = r.int()
        val vTrax = r.int()
        return SetSoundSettingsIn(vSystem, vFurni, vTrax)
    }

    private fun writeSetSoundSettingsIn(w: PacketWriter, m: SetSoundSettingsIn) {
        w.int(m.system)
        w.int(m.furni)
        w.int(m.trax)
    }
}
