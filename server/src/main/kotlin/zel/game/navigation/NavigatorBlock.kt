package zel.game.navigation

import zel.protocol.BodyPart
import zel.protocol.generated.PacketWriter
import zel.room.listing.ListedRoom

data class NavigatorBlock(
    val code: String,
    val text: String,
    val rooms: List<ListedRoom>,
    val action: SearchAction = SearchAction.NONE,
    val layout: BlockStyle = BlockStyle.ROWS,
    val display: DisplayMode = DisplayMode.VISIBLE,
    val narrowsOnText: Boolean = false,
    val showsHidden: Boolean = true,
    val rank: Int = 0,
    val roomOrder: Comparator<ListedRoom> = naturalOrder(),
) : BodyPart {
    val shownRooms: List<ListedRoom>
        get() = rooms.filter { showsHidden || it.doorCode != HIDDEN_DOOR }.sortedWith(roomOrder)

    fun keeping(keep: (ListedRoom) -> Boolean): NavigatorBlock = copy(rooms = rooms.filter(keep))

    fun narrowedTo(
        field: RoomSearchField,
        value: String,
    ): NavigatorBlock = if (narrowsOnText) keeping { field.keeps(it, value) } else this

    override fun writeTo(body: PacketWriter) {
        val shown = shownRooms
        body.string(code)
        body.string(text)
        body.int(action.type)
        body.boolean(display == DisplayMode.COLLAPSED)
        body.int(layout.code)
        body.int(shown.size)
        shown.forEach { it.writeTo(body) }
    }

    private companion object {
        const val HIDDEN_DOOR = 3
    }
}
