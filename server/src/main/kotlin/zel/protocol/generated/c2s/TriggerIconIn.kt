package zel.protocol.generated

data class TriggerIconIn(
    val iconId: Int,
)

object TriggerIconInCodec : IncomingCodec<TriggerIconIn> {
    override val name = "TriggerIconIn"

    override fun read(r: PacketReader): TriggerIconIn = readTriggerIconIn(r)

    override fun write(w: PacketWriter, m: TriggerIconIn) = writeTriggerIconIn(w, m)

    private fun readTriggerIconIn(r: PacketReader): TriggerIconIn {
        val vIconId = r.int()
        return TriggerIconIn(vIconId)
    }

    private fun writeTriggerIconIn(w: PacketWriter, m: TriggerIconIn) {
        w.int(m.iconId)
    }
}
