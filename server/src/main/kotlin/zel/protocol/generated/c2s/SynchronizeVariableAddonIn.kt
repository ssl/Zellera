package zel.protocol.generated

data class SynchronizeVariableAddonIn(
    val furniId: Int,
)

object SynchronizeVariableAddonInCodec : IncomingCodec<SynchronizeVariableAddonIn> {
    override val name = "SynchronizeVariableAddonIn"

    override fun read(r: PacketReader): SynchronizeVariableAddonIn = readSynchronizeVariableAddonIn(r)

    override fun write(w: PacketWriter, m: SynchronizeVariableAddonIn) = writeSynchronizeVariableAddonIn(w, m)

    private fun readSynchronizeVariableAddonIn(r: PacketReader): SynchronizeVariableAddonIn {
        val vFurniId = r.int()
        return SynchronizeVariableAddonIn(vFurniId)
    }

    private fun writeSynchronizeVariableAddonIn(w: PacketWriter, m: SynchronizeVariableAddonIn) {
        w.int(m.furniId)
    }
}
