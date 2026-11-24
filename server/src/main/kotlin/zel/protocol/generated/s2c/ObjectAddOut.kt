package zel.protocol.generated

data class ObjectAddOut(
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
    val string3: String,
) {
    sealed interface Branch1 {
        data class Then(
            val string1: String,
        ) : Branch1
        data object Else : Branch1
    }
}

object ObjectAddOutCodec : OutgoingCodec<ObjectAddOut> {
    override val name = "ObjectAddOut"

    override fun read(r: PacketReader): ObjectAddOut = readObjectAddOut(r)

    override fun write(w: PacketWriter, m: ObjectAddOut) = writeObjectAddOut(w, m)

    private fun readObjectAddOutBranch1Then(r: PacketReader): ObjectAddOut.Branch1.Then {
        val vString1 = r.string()
        return ObjectAddOut.Branch1.Then(vString1)
    }

    private fun readObjectAddOut(r: PacketReader): ObjectAddOut {
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
        val vBranch1 = if (vInt2 < 0) readObjectAddOutBranch1Then(r) else ObjectAddOut.Branch1.Else
        val vString3 = r.string()
        return ObjectAddOut(vInt1, vInt2, vInt3, vInt4, vInt5, vString1, vString2, vInt6, vStuff1, vInt7, vInt8, vInt9, vBranch1, vString3)
    }

    private fun writeObjectAddOutBranch1Then(w: PacketWriter, m: ObjectAddOut.Branch1.Then) {
        w.string(m.string1)
    }

    private fun writeObjectAddOut(w: PacketWriter, m: ObjectAddOut) {
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
            is ObjectAddOut.Branch1.Then -> writeObjectAddOutBranch1Then(w, b)
            is ObjectAddOut.Branch1.Else -> {}
        }
        w.string(m.string3)
    }
}
