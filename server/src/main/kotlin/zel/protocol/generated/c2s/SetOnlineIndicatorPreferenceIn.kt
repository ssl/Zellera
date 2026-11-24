package zel.protocol.generated

data class SetOnlineIndicatorPreferenceIn(
    val preference: Int,
)

object SetOnlineIndicatorPreferenceInCodec : IncomingCodec<SetOnlineIndicatorPreferenceIn> {
    override val name = "SetOnlineIndicatorPreferenceIn"

    override fun read(r: PacketReader): SetOnlineIndicatorPreferenceIn = readSetOnlineIndicatorPreferenceIn(r)

    override fun write(w: PacketWriter, m: SetOnlineIndicatorPreferenceIn) = writeSetOnlineIndicatorPreferenceIn(w, m)

    private fun readSetOnlineIndicatorPreferenceIn(r: PacketReader): SetOnlineIndicatorPreferenceIn {
        val vPreference = r.int()
        return SetOnlineIndicatorPreferenceIn(vPreference)
    }

    private fun writeSetOnlineIndicatorPreferenceIn(w: PacketWriter, m: SetOnlineIndicatorPreferenceIn) {
        w.int(m.preference)
    }
}
