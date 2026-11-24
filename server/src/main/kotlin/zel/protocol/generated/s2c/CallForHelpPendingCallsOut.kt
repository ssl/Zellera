package zel.protocol.generated

data class CallForHelpPendingCallsOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val string1: String,
        val string2: String,
        val string3: String,
    )
}

object CallForHelpPendingCallsOutCodec : OutgoingCodec<CallForHelpPendingCallsOut> {
    override val name = "CallForHelpPendingCallsOut"

    override fun read(r: PacketReader): CallForHelpPendingCallsOut = readCallForHelpPendingCallsOut(r)

    override fun write(w: PacketWriter, m: CallForHelpPendingCallsOut) = writeCallForHelpPendingCallsOut(w, m)

    private fun readCallForHelpPendingCallsOutList1Item(r: PacketReader): CallForHelpPendingCallsOut.List1Item {
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        return CallForHelpPendingCallsOut.List1Item(vString1, vString2, vString3)
    }

    private fun readCallForHelpPendingCallsOut(r: PacketReader): CallForHelpPendingCallsOut {
        val vList1 = List(r.countInt(6)) { readCallForHelpPendingCallsOutList1Item(r) }
        return CallForHelpPendingCallsOut(vList1)
    }

    private fun writeCallForHelpPendingCallsOutList1Item(w: PacketWriter, m: CallForHelpPendingCallsOut.List1Item) {
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
    }

    private fun writeCallForHelpPendingCallsOut(w: PacketWriter, m: CallForHelpPendingCallsOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeCallForHelpPendingCallsOutList1Item(w, it) }
    }
}
