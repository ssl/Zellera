package zel.protocol.generated

data object GetSeasonalCalendarDailyIn

object GetSeasonalCalendarDailyInCodec : IncomingCodec<GetSeasonalCalendarDailyIn> {
    override val name = "GetSeasonalCalendarDailyIn"

    override fun read(r: PacketReader): GetSeasonalCalendarDailyIn = GetSeasonalCalendarDailyIn

    override fun write(w: PacketWriter, m: GetSeasonalCalendarDailyIn) = writeGetSeasonalCalendarDailyIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetSeasonalCalendarDailyIn(w: PacketWriter, m: GetSeasonalCalendarDailyIn) {
    }
}
