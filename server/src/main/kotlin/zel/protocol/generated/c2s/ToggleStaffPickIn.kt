package zel.protocol.generated

data class ToggleStaffPickIn(
    val int1: Int,
    val boolean1: Boolean,
)

object ToggleStaffPickInCodec : IncomingCodec<ToggleStaffPickIn> {
    override val name = "ToggleStaffPickIn"

    override fun read(r: PacketReader): ToggleStaffPickIn = readToggleStaffPickIn(r)

    override fun write(w: PacketWriter, m: ToggleStaffPickIn) = writeToggleStaffPickIn(w, m)

    private fun readToggleStaffPickIn(r: PacketReader): ToggleStaffPickIn {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        return ToggleStaffPickIn(vInt1, vBoolean1)
    }

    private fun writeToggleStaffPickIn(w: PacketWriter, m: ToggleStaffPickIn) {
        w.int(m.int1)
        w.boolean(m.boolean1)
    }
}
