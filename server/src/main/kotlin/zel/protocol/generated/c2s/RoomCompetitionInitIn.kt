package zel.protocol.generated

data object RoomCompetitionInitIn

object RoomCompetitionInitInCodec : IncomingCodec<RoomCompetitionInitIn> {
    override val name = "RoomCompetitionInitIn"

    override fun read(r: PacketReader): RoomCompetitionInitIn = RoomCompetitionInitIn

    override fun write(w: PacketWriter, m: RoomCompetitionInitIn) = writeRoomCompetitionInitIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeRoomCompetitionInitIn(w: PacketWriter, m: RoomCompetitionInitIn) {
    }
}
