package zel.protocol.generated

data class ChestPreferencesUpdateSuccessOut(
    val int1: Int,
    val boolean1: Boolean,
)

object ChestPreferencesUpdateSuccessOutCodec : OutgoingCodec<ChestPreferencesUpdateSuccessOut> {
    override val name = "ChestPreferencesUpdateSuccessOut"

    override fun read(r: PacketReader): ChestPreferencesUpdateSuccessOut = readChestPreferencesUpdateSuccessOut(r)

    override fun write(w: PacketWriter, m: ChestPreferencesUpdateSuccessOut) = writeChestPreferencesUpdateSuccessOut(w, m)

    private fun readChestPreferencesUpdateSuccessOut(r: PacketReader): ChestPreferencesUpdateSuccessOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        return ChestPreferencesUpdateSuccessOut(vInt1, vBoolean1)
    }

    private fun writeChestPreferencesUpdateSuccessOut(w: PacketWriter, m: ChestPreferencesUpdateSuccessOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
    }
}
