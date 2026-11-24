package zel.protocol.generated

data class SetChatStylePreferenceIn(
    val styleId: Int,
    val variant: Int,
)

object SetChatStylePreferenceInCodec : IncomingCodec<SetChatStylePreferenceIn> {
    override val name = "SetChatStylePreferenceIn"

    override fun read(r: PacketReader): SetChatStylePreferenceIn = readSetChatStylePreferenceIn(r)

    override fun write(w: PacketWriter, m: SetChatStylePreferenceIn) = writeSetChatStylePreferenceIn(w, m)

    private fun readSetChatStylePreferenceIn(r: PacketReader): SetChatStylePreferenceIn {
        val vStyleId = r.int()
        val vVariant = r.int()
        return SetChatStylePreferenceIn(vStyleId, vVariant)
    }

    private fun writeSetChatStylePreferenceIn(w: PacketWriter, m: SetChatStylePreferenceIn) {
        w.int(m.styleId)
        w.int(m.variant)
    }
}
