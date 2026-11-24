package zel.protocol.generated

data class RoomDimmerSavePresetIn(
    val int1: Int,
    val int2: Int,
    val string1: String,
    val int3: Int,
    val boolean1: Boolean,
    val boolean2: Boolean,
    val int4: Int,
)

object RoomDimmerSavePresetInCodec : IncomingCodec<RoomDimmerSavePresetIn> {
    override val name = "RoomDimmerSavePresetIn"

    override fun read(r: PacketReader): RoomDimmerSavePresetIn = readRoomDimmerSavePresetIn(r)

    override fun write(w: PacketWriter, m: RoomDimmerSavePresetIn) = writeRoomDimmerSavePresetIn(w, m)

    private fun readRoomDimmerSavePresetIn(r: PacketReader): RoomDimmerSavePresetIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vInt4 = r.int()
        return RoomDimmerSavePresetIn(vInt1, vInt2, vString1, vInt3, vBoolean1, vBoolean2, vInt4)
    }

    private fun writeRoomDimmerSavePresetIn(w: PacketWriter, m: RoomDimmerSavePresetIn) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        w.int(m.int4)
    }
}
