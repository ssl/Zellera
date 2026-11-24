package zel.protocol.generated

data class ObjectUpdateOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val string1: String,
    val string2: String,
    val int6: Int,
    val stuff1: StuffData,
    val int7: Int,
    val int8: Int,
    val int9: Int,
    val branch1: Branch1,
) {
    sealed interface Branch1 {
        data class Then(
            val string1: String,
        ) : Branch1
        data object Else : Branch1
    }
}

object ObjectUpdateOutCodec : OutgoingCodec<ObjectUpdateOut> {
    override val name = "ObjectUpdateOut"

    override fun read(r: PacketReader): ObjectUpdateOut = readObjectUpdateOut(r)

    override fun write(w: PacketWriter, m: ObjectUpdateOut) = writeObjectUpdateOut(w, m)

    private fun readObjectUpdateOutBranch1Then(r: PacketReader): ObjectUpdateOut.Branch1.Then {
        val vString1 = r.string()
        return ObjectUpdateOut.Branch1.Then(vString1)
    }

    private fun readObjectUpdateOut(r: PacketReader): ObjectUpdateOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt6 = r.int()
        val vStuff1 = StuffData.read(r)
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vInt9 = r.int()
        val vBranch1 = if (vInt2 < 0) readObjectUpdateOutBranch1Then(r) else ObjectUpdateOut.Branch1.Else
        return ObjectUpdateOut(vInt1, vInt2, vInt3, vInt4, vInt5, vString1, vString2, vInt6, vStuff1, vInt7, vInt8, vInt9, vBranch1)
    }

    private fun writeObjectUpdateOutBranch1Then(w: PacketWriter, m: ObjectUpdateOut.Branch1.Then) {
        w.string(m.string1)
    }

    private fun writeObjectUpdateOut(w: PacketWriter, m: ObjectUpdateOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int6)
        StuffData.write(w, m.stuff1)
        w.int(m.int7)
        w.int(m.int8)
        w.int(m.int9)
        when (val b = m.branch1) {
            is ObjectUpdateOut.Branch1.Then -> writeObjectUpdateOutBranch1Then(w, b)
            is ObjectUpdateOut.Branch1.Else -> {}
        }
    }
}
