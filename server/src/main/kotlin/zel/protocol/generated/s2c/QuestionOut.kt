package zel.protocol.generated

data class QuestionOut(
    val string1: String,
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val int6: Int,
    val string2: String,
    val branch1: Branch1,
) {
    sealed interface Branch1 {
        data class Then(
            val int1: Int,
            val list1: List<List1Item>,
        ) : Branch1 {
            data class List1Item(
                val string1: String,
                val string2: String,
            )
        }
        data object Else : Branch1
    }
}

object QuestionOutCodec : OutgoingCodec<QuestionOut> {
    override val name = "QuestionOut"

    override fun read(r: PacketReader): QuestionOut = readQuestionOut(r)

    override fun write(w: PacketWriter, m: QuestionOut) = writeQuestionOut(w, m)

    private fun readQuestionOutBranch1ThenList1Item(r: PacketReader): QuestionOut.Branch1.Then.List1Item {
        val vString1 = r.string()
        val vString2 = r.string()
        return QuestionOut.Branch1.Then.List1Item(vString1, vString2)
    }

    private fun readQuestionOutBranch1Then(r: PacketReader): QuestionOut.Branch1.Then {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(4)) { readQuestionOutBranch1ThenList1Item(r) }
        return QuestionOut.Branch1.Then(vInt1, vList1)
    }

    private fun readQuestionOut(r: PacketReader): QuestionOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vString2 = r.string()
        val vBranch1 = if (vInt6 in setOf(1, 2)) readQuestionOutBranch1Then(r) else QuestionOut.Branch1.Else
        return QuestionOut(vString1, vInt1, vInt2, vInt3, vInt4, vInt5, vInt6, vString2, vBranch1)
    }

    private fun writeQuestionOutBranch1ThenList1Item(w: PacketWriter, m: QuestionOut.Branch1.Then.List1Item) {
        w.string(m.string1)
        w.string(m.string2)
    }

    private fun writeQuestionOutBranch1Then(w: PacketWriter, m: QuestionOut.Branch1.Then) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeQuestionOutBranch1ThenList1Item(w, it) }
    }

    private fun writeQuestionOut(w: PacketWriter, m: QuestionOut) {
        w.string(m.string1)
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.string(m.string2)
        when (val b = m.branch1) {
            is QuestionOut.Branch1.Then -> writeQuestionOutBranch1Then(w, b)
            is QuestionOut.Branch1.Else -> {}
        }
    }
}
