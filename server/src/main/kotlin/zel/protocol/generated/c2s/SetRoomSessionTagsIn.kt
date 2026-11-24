package zel.protocol.generated

data class SetRoomSessionTagsIn(
    val string1: String,
    val string2: String,
)

object SetRoomSessionTagsInCodec : IncomingCodec<SetRoomSessionTagsIn> {
    override val name = "SetRoomSessionTagsIn"

    override fun read(r: PacketReader): SetRoomSessionTagsIn = readSetRoomSessionTagsIn(r)

    override fun write(w: PacketWriter, m: SetRoomSessionTagsIn) = writeSetRoomSessionTagsIn(w, m)

    private fun readSetRoomSessionTagsIn(r: PacketReader): SetRoomSessionTagsIn {
        val vString1 = r.string()
        val vString2 = r.string()
        return SetRoomSessionTagsIn(vString1, vString2)
    }

    private fun writeSetRoomSessionTagsIn(w: PacketWriter, m: SetRoomSessionTagsIn) {
        w.string(m.string1)
        w.string(m.string2)
    }
}
