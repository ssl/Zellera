package zel.protocol.generated

data class SetChatPreferencesIn(
    val unused: Boolean,
    val chatMode: Int,
    val bubbleWidth: Int,
    val scrollSpeed: Int,
)

object SetChatPreferencesInCodec : IncomingCodec<SetChatPreferencesIn> {
    override val name = "SetChatPreferencesIn"

    override fun read(r: PacketReader): SetChatPreferencesIn = readSetChatPreferencesIn(r)

    override fun write(w: PacketWriter, m: SetChatPreferencesIn) = writeSetChatPreferencesIn(w, m)

    private fun readSetChatPreferencesIn(r: PacketReader): SetChatPreferencesIn {
        val vUnused = r.boolean()
        val vChatMode = r.int()
        val vBubbleWidth = r.int()
        val vScrollSpeed = r.int()
        return SetChatPreferencesIn(vUnused, vChatMode, vBubbleWidth, vScrollSpeed)
    }

    private fun writeSetChatPreferencesIn(w: PacketWriter, m: SetChatPreferencesIn) {
        w.boolean(m.unused)
        w.int(m.chatMode)
        w.int(m.bubbleWidth)
        w.int(m.scrollSpeed)
    }
}
