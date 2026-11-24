package zel.protocol.generated

data class PhoneCollectionStateOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
)

object PhoneCollectionStateOutCodec : OutgoingCodec<PhoneCollectionStateOut> {
    override val name = "PhoneCollectionStateOut"

    override fun read(r: PacketReader): PhoneCollectionStateOut = readPhoneCollectionStateOut(r)

    override fun write(w: PacketWriter, m: PhoneCollectionStateOut) = writePhoneCollectionStateOut(w, m)

    private fun readPhoneCollectionStateOut(r: PacketReader): PhoneCollectionStateOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return PhoneCollectionStateOut(vInt1, vInt2, vInt3)
    }

    private fun writePhoneCollectionStateOut(w: PacketWriter, m: PhoneCollectionStateOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
