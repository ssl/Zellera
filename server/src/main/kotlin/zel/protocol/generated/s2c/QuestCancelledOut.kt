package zel.protocol.generated

data class QuestCancelledOut(
    val boolean1: Boolean,
    val string1: String,
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val boolean2: Boolean,
    val string2: String,
    val string3: String,
    val int5: Int,
    val string4: String,
    val int6: Int,
    val int7: Int,
    val int8: Int,
    val string5: String,
    val string6: String,
    val boolean3: Boolean,
    val boolean4: Boolean,
    val branch1: Branch1,
) {
    sealed interface Branch1 {
        data class Then(
            val int1: Int,
        ) : Branch1
        data object Else : Branch1
    }
}

object QuestCancelledOutCodec : OutgoingCodec<QuestCancelledOut> {
    override val name = "QuestCancelledOut"

    override fun read(r: PacketReader): QuestCancelledOut = readQuestCancelledOut(r)

    override fun write(w: PacketWriter, m: QuestCancelledOut) = writeQuestCancelledOut(w, m)

    private fun readQuestCancelledOutBranch1Then(r: PacketReader): QuestCancelledOut.Branch1.Then {
        val vInt1 = r.int()
        return QuestCancelledOut.Branch1.Then(vInt1)
    }

    private fun readQuestCancelledOut(r: PacketReader): QuestCancelledOut {
        val vBoolean1 = r.boolean()
        val vString1 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vBoolean2 = r.boolean()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt5 = r.int()
        val vString4 = r.string()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vString5 = r.string()
        val vString6 = r.string()
        val vBoolean3 = r.boolean()
        val vBoolean4 = r.boolean()
        val vBranch1 = if (vBoolean4 == true) readQuestCancelledOutBranch1Then(r) else QuestCancelledOut.Branch1.Else
        return QuestCancelledOut(vBoolean1, vString1, vInt1, vInt2, vInt3, vInt4, vBoolean2, vString2, vString3, vInt5, vString4, vInt6, vInt7, vInt8, vString5, vString6, vBoolean3, vBoolean4, vBranch1)
    }

    private fun writeQuestCancelledOutBranch1Then(w: PacketWriter, m: QuestCancelledOut.Branch1.Then) {
        w.int(m.int1)
    }

    private fun writeQuestCancelledOut(w: PacketWriter, m: QuestCancelledOut) {
        w.boolean(m.boolean1)
        w.string(m.string1)
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.boolean(m.boolean2)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int5)
        w.string(m.string4)
        w.int(m.int6)
        w.int(m.int7)
        w.int(m.int8)
        w.string(m.string5)
        w.string(m.string6)
        w.boolean(m.boolean3)
        w.boolean(m.boolean4)
        when (val b = m.branch1) {
            is QuestCancelledOut.Branch1.Then -> writeQuestCancelledOutBranch1Then(w, b)
            is QuestCancelledOut.Branch1.Else -> {}
        }
    }
}
