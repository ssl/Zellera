package zel.protocol.generated

data class UniqueIDIn(
    val storedMachineId: String,
    val string1: String,
    val string2: String,
)

object UniqueIDInCodec : IncomingCodec<UniqueIDIn> {
    override val name = "UniqueIDIn"

    override fun read(r: PacketReader): UniqueIDIn = readUniqueIDIn(r)

    override fun write(w: PacketWriter, m: UniqueIDIn) = writeUniqueIDIn(w, m)

    private fun readUniqueIDIn(r: PacketReader): UniqueIDIn {
        val vStoredMachineId = r.string()
        val vString1 = r.string()
        val vString2 = r.string()
        return UniqueIDIn(vStoredMachineId, vString1, vString2)
    }

    private fun writeUniqueIDIn(w: PacketWriter, m: UniqueIDIn) {
        w.string(m.storedMachineId)
        w.string(m.string1)
        w.string(m.string2)
    }
}
