package zel.protocol.generated

data class TogglePetBreedingPermissionIn(
    val petId: Int,
)

object TogglePetBreedingPermissionInCodec : IncomingCodec<TogglePetBreedingPermissionIn> {
    override val name = "TogglePetBreedingPermissionIn"

    override fun read(r: PacketReader): TogglePetBreedingPermissionIn = readTogglePetBreedingPermissionIn(r)

    override fun write(w: PacketWriter, m: TogglePetBreedingPermissionIn) = writeTogglePetBreedingPermissionIn(w, m)

    private fun readTogglePetBreedingPermissionIn(r: PacketReader): TogglePetBreedingPermissionIn {
        val vPetId = r.int()
        return TogglePetBreedingPermissionIn(vPetId)
    }

    private fun writeTogglePetBreedingPermissionIn(w: PacketWriter, m: TogglePetBreedingPermissionIn) {
        w.int(m.petId)
    }
}
