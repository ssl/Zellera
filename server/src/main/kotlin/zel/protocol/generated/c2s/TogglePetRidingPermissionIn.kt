package zel.protocol.generated

data class TogglePetRidingPermissionIn(
    val petId: Int,
)

object TogglePetRidingPermissionInCodec : IncomingCodec<TogglePetRidingPermissionIn> {
    override val name = "TogglePetRidingPermissionIn"

    override fun read(r: PacketReader): TogglePetRidingPermissionIn = readTogglePetRidingPermissionIn(r)

    override fun write(w: PacketWriter, m: TogglePetRidingPermissionIn) = writeTogglePetRidingPermissionIn(w, m)

    private fun readTogglePetRidingPermissionIn(r: PacketReader): TogglePetRidingPermissionIn {
        val vPetId = r.int()
        return TogglePetRidingPermissionIn(vPetId)
    }

    private fun writeTogglePetRidingPermissionIn(w: PacketWriter, m: TogglePetRidingPermissionIn) {
        w.int(m.petId)
    }
}
