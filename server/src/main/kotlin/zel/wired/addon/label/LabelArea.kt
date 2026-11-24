package zel.wired.addon.label

import zel.room.Room
import zel.room.RoomRegistry
import zel.room.entry.EntryViews
import zel.room.furni.FurniBehaviour
import zel.wired.store.RoomBoxForms
import zel.wired.store.WiredWriter
import javax.sql.DataSource

class LabelArea(
    dataSource: DataSource,
    writer: WiredWriter,
    private val forms: RoomBoxForms,
    private val levels: (Room) -> Map<Int, Int>,
    private val filterText: (Int, String) -> String,
) {
    val labels = RoomLabels(JdbcLabelStore(dataSource), writer)
    private val out = LabelOut(forms, levels)

    val behaviours: List<Pair<String, FurniBehaviour>> = listOf(Label.CODE to LabelFurni(labels, out))

    fun packets(registry: RoomRegistry): LabelPackets = LabelPackets(registry, labels, out, forms, levels, filterText)

    fun views(next: EntryViews): EntryViews = LabelEntryViews(labels, out, next)
}
