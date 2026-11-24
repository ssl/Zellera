package zel.protocol.generated

data class PassCarryItemToPetIn(
    val petId: Int,
)

object PassCarryItemToPetInCodec : IncomingCodec<PassCarryItemToPetIn> {
    override val name = "PassCarryItemToPetIn"

    override fun read(r: PacketReader): PassCarryItemToPetIn = readPassCarryItemToPetIn(r)

    override fun write(w: PacketWriter, m: PassCarryItemToPetIn) = writePassCarryItemToPetIn(w, m)

    private fun readPassCarryItemToPetIn(r: PacketReader): PassCarryItemToPetIn {
        val vPetId = r.int()
        return PassCarryItemToPetIn(vPetId)
    }

    private fun writePassCarryItemToPetIn(w: PacketWriter, m: PassCarryItemToPetIn) {
        w.int(m.petId)
    }
}
