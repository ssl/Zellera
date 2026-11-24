package zel.protocol.generated

data class MountPetIn(
    val petId: Int,
    val boolean1: Boolean,
)

object MountPetInCodec : IncomingCodec<MountPetIn> {
    override val name = "MountPetIn"

    override fun read(r: PacketReader): MountPetIn = readMountPetIn(r)

    override fun write(w: PacketWriter, m: MountPetIn) = writeMountPetIn(w, m)

    private fun readMountPetIn(r: PacketReader): MountPetIn {
        val vPetId = r.int()
        val vBoolean1 = r.boolean()
        return MountPetIn(vPetId, vBoolean1)
    }

    private fun writeMountPetIn(w: PacketWriter, m: MountPetIn) {
        w.int(m.petId)
        w.boolean(m.boolean1)
    }
}
