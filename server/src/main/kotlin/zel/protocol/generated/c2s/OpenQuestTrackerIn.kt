package zel.protocol.generated

data object OpenQuestTrackerIn

object OpenQuestTrackerInCodec : IncomingCodec<OpenQuestTrackerIn> {
    override val name = "OpenQuestTrackerIn"

    override fun read(r: PacketReader): OpenQuestTrackerIn = OpenQuestTrackerIn

    override fun write(w: PacketWriter, m: OpenQuestTrackerIn) = writeOpenQuestTrackerIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeOpenQuestTrackerIn(w: PacketWriter, m: OpenQuestTrackerIn) {
    }
}
