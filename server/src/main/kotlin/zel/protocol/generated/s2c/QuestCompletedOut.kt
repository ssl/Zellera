package zel.protocol.generated

data class QuestCompletedOut(
    val string1: String,
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val boolean1: Boolean,
    val string2: String,
    val string3: String,
    val int5: Int,
    val string4: String,
    val int6: Int,
    val int7: Int,
    val int8: Int,
    val string5: String,
    val string6: String,
    val boolean2: Boolean,
    val boolean3: Boolean,
    val branch1: Branch1,
    val boolean4: Boolean,
) {
    sealed interface Branch1 {
        data class Then(
            val int1: Int,
        ) : Branch1
        data object Else : Branch1
    }
}

object QuestCompletedOutCodec : OutgoingCodec<QuestCompletedOut> {
    override val name = "QuestCompletedOut"

    override fun read(r: PacketReader): QuestCompletedOut = readQuestCompletedOut(r)

    override fun write(w: PacketWriter, m: QuestCompletedOut) = writeQuestCompletedOut(w, m)

    private fun readQuestCompletedOutBranch1Then(r: PacketReader): QuestCompletedOut.Branch1.Then {
        val vInt1 = r.int()
        return QuestCompletedOut.Branch1.Then(vInt1)
    }

    private fun readQuestCompletedOut(r: PacketReader): QuestCompletedOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vBoolean1 = r.boolean()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt5 = r.int()
        val vString4 = r.string()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vString5 = r.string()
        val vString6 = r.string()
        val vBoolean2 = r.boolean()
        val vBoolean3 = r.boolean()
        val vBranch1 = if (vBoolean3 == true) readQuestCompletedOutBranch1Then(r) else QuestCompletedOut.Branch1.Else
        val vBoolean4 = r.boolean()
        return QuestCompletedOut(vString1, vInt1, vInt2, vInt3, vInt4, vBoolean1, vString2, vString3, vInt5, vString4, vInt6, vInt7, vInt8, vString5, vString6, vBoolean2, vBoolean3, vBranch1, vBoolean4)
    }

    private fun writeQuestCompletedOutBranch1Then(w: PacketWriter, m: QuestCompletedOut.Branch1.Then) {
        w.int(m.int1)
    }

    private fun writeQuestCompletedOut(w: PacketWriter, m: QuestCompletedOut) {
        w.string(m.string1)
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.boolean(m.boolean1)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int5)
        w.string(m.string4)
        w.int(m.int6)
        w.int(m.int7)
        w.int(m.int8)
        w.string(m.string5)
        w.string(m.string6)
        w.boolean(m.boolean2)
        w.boolean(m.boolean3)
        when (val b = m.branch1) {
            is QuestCompletedOut.Branch1.Then -> writeQuestCompletedOutBranch1Then(w, b)
            is QuestCompletedOut.Branch1.Else -> {}
        }
        w.boolean(m.boolean4)
    }
}
