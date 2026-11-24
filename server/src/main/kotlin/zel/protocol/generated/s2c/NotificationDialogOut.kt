package zel.protocol.generated

data class NotificationDialogOut(
    val string1: String,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val string1: String,
        val string2: String,
    )
}

object NotificationDialogOutCodec : OutgoingCodec<NotificationDialogOut> {
    override val name = "NotificationDialogOut"

    override fun read(r: PacketReader): NotificationDialogOut = readNotificationDialogOut(r)

    override fun write(w: PacketWriter, m: NotificationDialogOut) = writeNotificationDialogOut(w, m)

    private fun readNotificationDialogOutList1Item(r: PacketReader): NotificationDialogOut.List1Item {
        val vString1 = r.string()
        val vString2 = r.string()
        return NotificationDialogOut.List1Item(vString1, vString2)
    }

    private fun readNotificationDialogOut(r: PacketReader): NotificationDialogOut {
        val vString1 = r.string()
        val vList1 = List(r.countInt(4)) { readNotificationDialogOutList1Item(r) }
        return NotificationDialogOut(vString1, vList1)
    }

    private fun writeNotificationDialogOutList1Item(w: PacketWriter, m: NotificationDialogOut.List1Item) {
        w.string(m.string1)
        w.string(m.string2)
    }

    private fun writeNotificationDialogOut(w: PacketWriter, m: NotificationDialogOut) {
        w.string(m.string1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeNotificationDialogOutList1Item(w, it) }
    }
}
