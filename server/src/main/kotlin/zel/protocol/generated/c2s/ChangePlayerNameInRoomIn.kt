package zel.protocol.generated

data class ChangePlayerNameInRoomIn(
    val name: String,
)

object ChangePlayerNameInRoomInCodec : IncomingCodec<ChangePlayerNameInRoomIn> {
    override val name = "ChangePlayerNameInRoomIn"

    override fun read(r: PacketReader): ChangePlayerNameInRoomIn = readChangePlayerNameInRoomIn(r)

    override fun write(w: PacketWriter, m: ChangePlayerNameInRoomIn) = writeChangePlayerNameInRoomIn(w, m)

    private fun readChangePlayerNameInRoomIn(r: PacketReader): ChangePlayerNameInRoomIn {
        val vName = r.string()
        return ChangePlayerNameInRoomIn(vName)
    }

    private fun writeChangePlayerNameInRoomIn(w: PacketWriter, m: ChangePlayerNameInRoomIn) {
        w.string(m.name)
    }
}
