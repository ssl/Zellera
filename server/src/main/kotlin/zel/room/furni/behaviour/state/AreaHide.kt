package zel.room.furni.behaviour.state

import zel.protocol.generated.AreaHideOut
import zel.protocol.generated.AreaHideOutCodec
import zel.protocol.generated.FloorHeightMapOut
import zel.protocol.generated.HeightMapUpdateOutCodec
import zel.protocol.generated.ItemAddOutCodec
import zel.protocol.generated.ItemRemoveOut
import zel.protocol.generated.ItemRemoveOutCodec
import zel.protocol.generated.StuffData
import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.RoomHeights
import zel.room.furni.Toggleable
import zel.room.furni.Usable
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Placeable
import zel.room.furni.place.StuffOnWire
import zel.room.furni.place.WallPosition
import zel.room.map.MapMessages
import zel.room.map.Point
import zel.room.map.TileValue
import zel.room.tick.Audience
import zel.room.tick.Lane
import kotlin.random.Random

/**
 * The wall tiles an area hide with "hide wall items" ticked hides: its area, or everything outside it when inverted.
 * A wall item counts by the wall tile its position names (`:w=x,y`), which need not be a floor tile.
 */
data class WallArea(
    val x0: Int,
    val y0: Int,
    val width: Int,
    val length: Int,
    val invert: Boolean,
) {
    operator fun contains(tile: Point): Boolean =
        (tile.x in x0 until x0 + width && tile.y in y0 until y0 + length) != invert
}

/** A wired firing's flash, as an area hide's change goes out with it. */
interface ChangeFlash {
    /** After the firing's ObjectsDataUpdate: where a wired change puts its heights. */
    fun later(send: (RoomScope) -> Unit)

    /**
     * Before the firing's ObjectsDataUpdate, only the last [send] given for [key], in the order last given: an area
     * hide's view goes out once per firing with its final values.
     */
    fun once(
        key: Int,
        send: (RoomScope) -> Unit,
    )
}

class AreaHide(
    private val part: StatePart,
) : Usable,
    Toggleable,
    StuffOnWire,
    Placeable {
    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        if (!part.hooks.hasRights(scope, playerId)) return
        flip(scope, furni)
        StateShow.data(scope, furni, furni.state)
        refresh(scope, furni, announce = true)
    }

    override fun toggle(
        scope: RoomScope,
        furni: RoomFurni,
        backward: Boolean,
    ): Boolean {
        flip(scope, furni)
        refresh(scope, furni, announce = true)
        return true
    }

    override fun toggleRandom(
        scope: RoomScope,
        furni: RoomFurni,
        random: Random,
    ): Boolean = toggle(scope, furni, backward = false)

    /** The tiles a pickup gives their heights back: not those another area hide that is on still hides. */
    override fun released(
        scope: RoomScope,
        furni: RoomFurni,
    ): List<Point> {
        val elsewhere = hiddenTiles(part, scope.room, except = furni.id)
        return held(part, scope.room)[furni.id].orEmpty().filterNot { it in elsewhere }
    }

    /** Picked up while on: right after its remove, the furni it hid come back. */
    override fun onLifted(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        val walls = part.of(scope.room).hiddenWalls[furni.id]
        held(part, scope.room)[furni.id]?.let {
            InvisibleFurni.out(scope, furniOn(scope, furni, it), hide = false, boxesAtRest = true)
        }
        wallsOut(scope, wallsIn(scope, furni, walls), hide = false)
    }

    /** Picked up: after the heights, an area hide that was on sends AreaHide off. */
    override fun onTilesFreed(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        val values = values(furni)
        part.of(scope.room).hiddenWalls.remove(furni.id)
        val wasOn = held(part, scope.room).remove(furni.id) != null || values[STATE] == ON
        values[STATE] = OFF
        StateShow.set(furni, CustomValues.write(values))
        if (wasOn) announce(scope, furni)
    }

    /**
     * A wired `@state` write or match to snapshot: the same as switching it, only when it changes (the stored area
     * stays). Whether it changed.
     */
    fun switchTo(
        scope: RoomScope,
        furni: RoomFurni,
        on: Boolean,
        flash: ChangeFlash? = null,
    ): Boolean {
        if (isOn(furni) == on) return false
        flip(scope, furni)
        changed(scope, furni, flash)
        return true
    }

    /**
     * A wired write of one of its values (`~area_hide.*`): the view follows, then AreaHide, only when it changes.
     * Whether it changed.
     */
    fun write(
        scope: RoomScope,
        furni: RoomFurni,
        key: String,
        value: String,
        flash: ChangeFlash? = null,
    ): Boolean {
        held(part, scope.room)
        val values = values(furni)
        if (values[key] == value) return false
        values[key] = value
        StateShow.set(furni, CustomValues.write(values))
        changed(scope, furni, flash)
        return true
    }

    /**
     * Its view after a change: right away, or within a firing once, with the final values, before the firing's
     * ObjectsDataUpdate: one AreaHide per area hide, in the order they were last changed.
     */
    private fun changed(
        scope: RoomScope,
        furni: RoomFurni,
        flash: ChangeFlash?,
    ) {
        if (flash == null) {
            refresh(scope, furni, announce = true)
            return
        }
        flash.once(furni.id) { to ->
            if (to.room.furni[furni.id] === furni) refresh(to, furni, announce = true, later = flash::later)
        }
    }

    @Suppress("LongParameterList") // the message's own fields
    fun configure(
        scope: RoomScope,
        furni: RoomFurni,
        root: Point,
        width: Int,
        length: Int,
        invisibility: Boolean,
        wallItems: Boolean,
        invert: Boolean,
    ) {
        held(part, scope.room)
        val wasInvisible = InvisibleFurni.of(furni)
        val values = values(furni)
        values["rootX"] = root.x.toString()
        values["rootY"] = root.y.toString()
        values["width"] = width.toString()
        values["length"] = length.toString()
        values["invisibility"] = if (invisibility) ON else OFF
        values["wallItems"] = if (wallItems) ON else OFF
        values["invert"] = if (invert) ON else OFF
        StateShow.data(scope, furni, CustomValues.write(values))
        refresh(scope, furni, announce = false)
        InvisibleFurni.changed(part, scope, furni, wasInvisible)
    }

    override fun stuff(furni: RoomFurni): StuffData {
        val values = values(furni)
        return StuffData(INT_ARRAY_FORMAT, StuffData.Value.IntArray(KEYS.map { values[it]?.toIntOrNull() ?: 0 }))
    }

    /**
     * After its values changed: the furni on the tiles it starts hiding leave every view, those on the tiles it stops
     * hiding come back, then ([announce]) AreaHide, then the heights of the tiles that changed, on [later] when a
     * wired change sends them after its flash. A tile another area hide that is on still hides keeps its view. With
     * "hide wall items" ticked the wall items in its area go the same way, before AreaHide.
     */
    private fun refresh(
        scope: RoomScope,
        furni: RoomFurni,
        announce: Boolean,
        later: ((RoomScope) -> Unit) -> Unit = { it(scope) },
    ) {
        val on = values(furni)[STATE] == ON
        val hidden = held(part, scope.room)
        val before = hidden.remove(furni.id).orEmpty()
        val now = if (on) tiles(scope.room, furni) else emptyList()
        if (on) hidden[furni.id] = now
        val was = furniOn(scope, furni, before)
        val will = furniOn(scope, furni, now)
        InvisibleFurni.out(scope, will - was.toSet(), hide = true)
        InvisibleFurni.out(scope, was - will.toSet(), hide = false, boxesAtRest = true)
        val walls = part.of(scope.room).hiddenWalls
        val wallsWere = wallsIn(scope, furni, walls.remove(furni.id))
        val wallsNow = wallArea(furni)?.takeIf { on }
        if (wallsNow != null) walls[furni.id] = wallsNow
        val wallsWill = wallsIn(scope, furni, wallsNow)
        wallsOut(scope, wallsWill - wallsWere.toSet(), hide = true)
        wallsOut(scope, wallsWere - wallsWill.toSet(), hide = false)
        if (announce) announce(scope, furni)
        val elsewhere = hiddenTiles(part, scope.room, except = furni.id)
        val shown = (before - now.toSet()).filterNot { it in elsewhere }
        val covered = (now - before.toSet()).filterNot { it in elsewhere }
        val heights = shown.map { TileValue(it, RoomHeights.value(scope.room, it)) }
        val voids = covered.map { TileValue(it, MapMessages.VOID_VALUE) }
        val changes = (heights + voids).sortedWith(compareBy({ it.tile.y }, { it.tile.x }))
        val updates = MapMessages.heightMapUpdates(changes)
        if (updates.isEmpty()) return
        val send = { to: RoomScope ->
            updates.forEach { to.out.send(Lane.Now, Audience.Room, HeightMapUpdateOutCodec, it) }
        }
        later(send)
    }

    /**
     * The floor furni standing on [tiles], row by row: no area hide (an area hide never hides one, itself or another),
     * nor what the invisible furni controller or another area hide already keeps out of view.
     */
    private fun furniOn(
        scope: RoomScope,
        furni: RoomFurni,
        tiles: List<Point>,
    ): List<RoomFurni> {
        if (tiles.isEmpty()) return emptyList()
        val order = tiles.withIndex().associate { it.value to it.index }
        val controller = part.of(scope.room).furniHidden == true
        val elsewhere = hiddenTiles(part, scope.room, except = furni.id)
        return scope.room.furni
            .all()
            .filter { other ->
                val at = other.anchor
                other.behaviour !is AreaHide &&
                    !other.isWall &&
                    at != null &&
                    at in order &&
                    !(controller && InvisibleFurni.of(other)) &&
                    at !in elsewhere
            }.sortedBy { order.getValue(checkNotNull(it.anchor)) }
    }

    /** The wall items in [area], in the room's order (that of Items): not those another area hide keeps out. */
    private fun wallsIn(
        scope: RoomScope,
        furni: RoomFurni,
        area: WallArea?,
    ): List<RoomFurni> {
        if (area == null) return emptyList()
        val elsewhere =
            part
                .of(scope.room)
                .hiddenWalls
                .filterKeys { it != furni.id }
                .values
        return scope.room.furni.all().filter { other ->
            val tile = if (other.isWall) WallPosition.tile(other.wallPosition) else null
            tile != null && tile in area && elsewhere.none { tile in it }
        }
    }

    /** Wall items out of every view, one ItemRemove each (picker -1, true), or back with an ItemAdd each. */
    private fun wallsOut(
        scope: RoomScope,
        walls: List<RoomFurni>,
        hide: Boolean,
    ) = walls.forEach {
        if (hide) {
            val removal = ItemRemoveOut(it.id.toString(), NO_PICKER, true)
            scope.out.send(Lane.Now, Audience.Room, ItemRemoveOutCodec, removal)
        } else {
            scope.out.send(Lane.Now, Audience.Room, ItemAddOutCodec, FurniWire.itemAdd(it))
        }
    }

    private fun announce(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        val values = values(furni)

        fun int(key: String) = values[key]?.toIntOrNull() ?: 0
        scope.out.send(
            Lane.Now,
            Audience.Room,
            AreaHideOutCodec,
            AreaHideOut(
                furni.id,
                values[STATE] == ON,
                int("rootX"),
                int("rootY"),
                int("width"),
                int("length"),
                values["invert"] == ON,
            ),
        )
    }

    /** Reads what the room hides before the state changes, so a first use after a load starts from the stored one. */
    private fun flip(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        held(part, scope.room)
        val values = values(furni)
        values[STATE] = if (values[STATE] == ON) OFF else ON
        StateShow.set(furni, CustomValues.write(values))
    }

    companion object {
        /** Whether [furni] (an area hide) is on. */
        fun isOn(furni: RoomFurni): Boolean = values(furni)[STATE] == ON

        /**
         * Whether a saved snapshot state puts an area hide on. A snapshot keeps only its on/off flag: the `state` of a
         * stored `state=..;rootX=..;` text, or a bare `1` / `0`. Its area is never put back.
         */
        fun snapshotOn(saved: String): Boolean =
            if ('=' in saved) CustomValues.read(saved, emptyMap())[STATE] == ON else saved.trim() == ON

        /** Whether an area hide is as a saved snapshot state keeps it: on or off only. */
        fun matches(
            furni: RoomFurni,
            saved: String,
        ): Boolean = isOn(furni) == snapshotOn(saved)

        /**
         * Whether an area hide that is on keeps a furni out of view. An area hide never hides an area hide, its own
         * tile or another's: they all stay in view with their states. A wall item is kept out by the wall areas it
         * hides.
         */
        fun keepsOut(
            part: StatePart,
            room: Room,
        ): (RoomFurni) -> Boolean {
            val all = hiddenTiles(part, room)
            val walls = part.of(room).hiddenWalls.values
            return { furni ->
                if (furni.isWall) {
                    val tile = WallPosition.tile(furni.wallPosition)
                    tile != null && walls.any { tile in it }
                } else {
                    val at = furni.anchor
                    at != null && furni.behaviour !is AreaHide && at in all
                }
            }
        }

        /**
         * The tiles each area hide that is on hides. A room loaded with one on starts from the stored states, so its
         * viewers are sent the room without those furni and switching it off gives them back.
         */
        private fun held(
            part: StatePart,
            room: Room,
        ): HashMap<Int, List<Point>> {
            val states = part.of(room)
            if (!states.hiddenLoaded) {
                states.hiddenLoaded = true
                room.furni
                    .all()
                    .filter { it.behaviour is AreaHide && values(it)[STATE] == ON }
                    .forEach { states.hidden[it.id] = tiles(room, it) }
            }
            return states.hidden
        }

        /**
         * The FloorHeightMap area list of an entry: each area hide that is on, with the fields its AreaHide message
         * carries.
         */
        fun entryAreas(
            part: StatePart,
            room: Room,
        ): List<FloorHeightMapOut.List1Item> =
            held(part, room).keys.mapNotNull { room.furni[it] }.map { furni ->
                val values = values(furni)

                fun int(key: String) = values[key]?.toIntOrNull() ?: 0
                FloorHeightMapOut.List1Item(
                    furni.id,
                    true,
                    int("rootX"),
                    int("rootY"),
                    int("width"),
                    int("length"),
                    values["invert"] == ON,
                )
            }

        /** The floor tiles the area hides that are on hide: the room's viewers have them as void. */
        fun hiddenFloor(
            part: StatePart,
            room: Room,
        ): Set<Point> = hiddenTiles(part, room)

        /** The tiles the area hides that are on (but [except]) hide; a furni standing on one is out of view. */
        private fun hiddenTiles(
            part: StatePart,
            room: Room,
            except: Int? = null,
        ): Set<Point> =
            held(part, room)
                .filterKeys { it != except }
                .values
                .flatten()
                .toHashSet()

        private fun tiles(
            room: Room,
            furni: RoomFurni,
        ): List<Point> {
            val values = values(furni)

            fun int(key: String) = maxOf(0, values[key]?.toIntOrNull() ?: 0)
            val x0 = int("rootX")
            val y0 = int("rootY")
            val inside = { p: Point -> p.x in x0 until x0 + int("width") && p.y in y0 until y0 + int("length") }
            val floor = room.floor
            val rows = floor.tiles().filter { floor.isFloor(it) }.sortedWith(compareBy({ it.y }, { it.x }))
            return if (values["invert"] == ON) rows.filterNot(inside) else rows.filter(inside)
        }

        /** The wall area it hides while on: none with "hide wall items" unticked. */
        private fun wallArea(furni: RoomFurni): WallArea? {
            val values = values(furni)
            if (values[WALL_ITEMS] != ON) return null

            fun int(key: String) = maxOf(0, values[key]?.toIntOrNull() ?: 0)
            return WallArea(int("rootX"), int("rootY"), int("width"), int("length"), values["invert"] == ON)
        }

        private fun values(furni: RoomFurni): LinkedHashMap<String, String> =
            CustomValues.read(furni.state, DEFAULTS).also { values ->
                KEYS.forEach { key -> if (values[key]?.toIntOrNull() == null) values[key] = DEFAULTS.getValue(key) }
            }

        private const val INT_ARRAY_FORMAT = 5
        private const val STATE = "state"
        private const val ON = "1"
        private const val OFF = "0"
        private const val WALL_ITEMS = "wallItems"
        private const val NO_PICKER = -1
        private val KEYS = listOf(STATE, "rootX", "rootY", "width", "length", "invisibility", "wallItems", "invert")
        private val DEFAULTS =
            linkedMapOf(
                STATE to OFF,
                "rootX" to "0",
                "rootY" to "0",
                "width" to "0",
                "length" to "0",
                "invisibility" to "0",
                "wallItems" to "1",
                "invert" to "0",
            )
    }
}
