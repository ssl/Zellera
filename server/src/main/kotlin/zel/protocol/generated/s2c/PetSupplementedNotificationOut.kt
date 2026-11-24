package zel.protocol.generated

data class PetSupplementedNotificationOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
)

object PetSupplementedNotificationOutCodec : OutgoingCodec<PetSupplementedNotificationOut> {
    override val name = "PetSupplementedNotificationOut"

    override fun read(r: PacketReader): PetSupplementedNotificationOut = readPetSupplementedNotificationOut(r)

    override fun write(w: PacketWriter, m: PetSupplementedNotificationOut) = writePetSupplementedNotificationOut(w, m)

    private fun readPetSupplementedNotificationOut(r: PacketReader): PetSupplementedNotificationOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return PetSupplementedNotificationOut(vInt1, vInt2, vInt3)
    }

    private fun writePetSupplementedNotificationOut(w: PacketWriter, m: PetSupplementedNotificationOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
