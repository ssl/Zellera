package zel.protocol.generated

data class SetActivatedBadgesIn(
    val slot1: Int,
    val badge1: String,
    val slot2: Int,
    val badge2: String,
    val slot3: Int,
    val badge3: String,
    val slot4: Int,
    val badge4: String,
    val slot5: Int,
    val badge5: String,
)

object SetActivatedBadgesInCodec : IncomingCodec<SetActivatedBadgesIn> {
    override val name = "SetActivatedBadgesIn"

    override fun read(r: PacketReader): SetActivatedBadgesIn = readSetActivatedBadgesIn(r)

    override fun write(w: PacketWriter, m: SetActivatedBadgesIn) = writeSetActivatedBadgesIn(w, m)

    private fun readSetActivatedBadgesIn(r: PacketReader): SetActivatedBadgesIn {
        val vSlot1 = r.int()
        val vBadge1 = r.string()
        val vSlot2 = r.int()
        val vBadge2 = r.string()
        val vSlot3 = r.int()
        val vBadge3 = r.string()
        val vSlot4 = r.int()
        val vBadge4 = r.string()
        val vSlot5 = r.int()
        val vBadge5 = r.string()
        return SetActivatedBadgesIn(vSlot1, vBadge1, vSlot2, vBadge2, vSlot3, vBadge3, vSlot4, vBadge4, vSlot5, vBadge5)
    }

    private fun writeSetActivatedBadgesIn(w: PacketWriter, m: SetActivatedBadgesIn) {
        w.int(m.slot1)
        w.string(m.badge1)
        w.int(m.slot2)
        w.string(m.badge2)
        w.int(m.slot3)
        w.string(m.badge3)
        w.int(m.slot4)
        w.string(m.badge4)
        w.int(m.slot5)
        w.string(m.badge5)
    }
}
