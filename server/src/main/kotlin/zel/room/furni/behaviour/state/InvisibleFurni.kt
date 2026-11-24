package zel.room.furni.behaviour.state

import zel.protocol.generated.ObjectRemoveMultipleOut
import zel.protocol.generated.ObjectRemoveMultipleOutCodec
import zel.protocol.generated.ObjectsOutCodec
import zel.room.Room
import zel.room.RoomScope
import zel.room.entry.EntryMessages
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.room.tick.Audience
import zel.room.tick.Lane

/**
 * The furni an invisible furni controller that is on takes out of every view. The room keeps them: units still
 * walk, stand and sit on them and wired still reaches them. The family: walk and stack tiles, the invisible blocks
 * and sit/lay tiles, room linkers and area hides; never wired boxes, antennas, team meters or the hand item block
 * (those an area hide hides). Call under the room lock.
 */
object InvisibleFurni {
    private val BEHAVIOURS =
        setOf("tile.stack", "tile.walk", "wf_room_linker")

    private val CODE_PREFIXES = listOf("room_invisible_")

    private const val AREA_HIDE = "area.hide"

    /** The area hide's "make this furniture invisible" option. */
    private const val INVISIBILITY = "invisibility"

    /** The remove's picker: nobody picked them up. */
    private const val NO_PICKER = -1

    fun of(furni: RoomFurni): Boolean {
        if (furni.isWall) return false
        val type = Interactions.of(furni.definition)
        val code = furni.definition.name.lowercase()
        return type in BEHAVIOURS ||
            CODE_PREFIXES.any(code::startsWith) ||
            (type == AREA_HIDE && CustomValues.read(furni.state, emptyMap())[INVISIBILITY] == RoomControllers.ON)
    }

    /**
     * [furni] as a viewer is sent them: without the invisible furni while the controller is on, nor the furni an area
     * hide that is on hides (an entry and a placement on a hidden tile).
     */
    fun shown(
        part: StatePart,
        room: Room,
        furni: Collection<RoomFurni>,
    ): Collection<RoomFurni> = furni.filter(inView(part, room))

    /**
     * Whether the room's viewers have a furni now: not one the controller that is on keeps out, nor one an area hide
     * that is on hides. The same for everyone in the room; built once per call, for the furni of one moment.
     */
    fun inView(
        part: StatePart,
        room: Room,
    ): (RoomFurni) -> Boolean {
        val byArea = AreaHide.keepsOut(part, room)
        val controller = hidden(part, room)
        return { furni -> !(controller && of(furni)) && !byArea(furni) }
    }

    /** After a controller changed: takes the invisible furni out of every view, or gives them back. */
    fun sync(
        part: StatePart,
        scope: RoomScope,
    ) {
        val states = part.of(scope.room)
        val was = states.furniHidden
        val now = RoomControllers.invisibleFurni(scope.room)
        states.furniHidden = now
        if (was == null || was == now) return
        val byArea = AreaHide.keepsOut(part, scope.room)
        val invisible =
            scope.room.furni
                .all()
                .filter { of(it) && !byArea(it) }
        out(scope, invisible, now)
    }

    /** [furni] became an invisible furni, or stopped being one ([was] what it was before). */
    fun changed(
        part: StatePart,
        scope: RoomScope,
        furni: RoomFurni,
        was: Boolean,
    ) {
        if (part.of(scope.room).furniHidden != true || of(furni) == was) return
        out(scope, listOf(furni), hide = !was)
    }

    private fun hidden(
        part: StatePart,
        room: Room,
    ): Boolean {
        val states = part.of(room)
        return states.furniHidden ?: RoomControllers.invisibleFurni(room).also { states.furniHidden = it }
    }

    /** Takes [furni] out of every view as a pickup does (an area hide's too), or gives them back. */
    fun out(
        scope: RoomScope,
        furni: List<RoomFurni>,
        hide: Boolean,
        boxesAtRest: Boolean = false,
    ) {
        val floor = furni.filter { !it.isWall && it.anchor != null }
        if (floor.isEmpty()) return
        if (hide) {
            val removal = ObjectRemoveMultipleOut(floor.map { it.id }, NO_PICKER, true)
            scope.out.send(Lane.Now, Audience.Room, ObjectRemoveMultipleOutCodec, removal)
        } else {
            scope.out.send(Lane.Now, Audience.Room, ObjectsOutCodec, EntryMessages.objects(floor, boxesAtRest))
        }
    }
}
