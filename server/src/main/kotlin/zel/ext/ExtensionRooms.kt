package zel.ext

import zel.game.items.Item
import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.map.FloorPlan
import zel.room.map.Point
import zel.room.unit.RoomUnit

@Suppress("TooManyFunctions") // one per action an extension may take
interface ExtensionRooms {
    fun roomOf(playerId: Int): Int?

    fun post(
        roomId: Int,
        delayMillis: Long = 0,
        block: (RoomScope) -> Unit,
    ): Boolean

    fun nameOf(
        scope: RoomScope,
        unit: RoomUnit,
    ): String?

    fun definition(code: String): Item?

    /** Lives only while the room is loaded: never saved. */
    @Suppress("LongParameterList") // where and how the furni stands
    fun addFurni(
        scope: RoomScope,
        definition: Item,
        tile: Point,
        z: Double,
        direction: Int,
        state: String,
    ): RoomFurni

    fun removeFurni(
        scope: RoomScope,
        furni: RoomFurni,
    )

    fun setState(
        scope: RoomScope,
        furni: RoomFurni,
        state: String,
    )

    fun moves(scope: RoomScope): Moves

    fun place(
        scope: RoomScope,
        unit: RoomUnit,
        tile: Point,
        z: Double? = null,
    )

    fun sit(
        scope: RoomScope,
        unit: RoomUnit,
        height: Double,
        direction: Int,
    )

    fun lie(
        scope: RoomScope,
        unit: RoomUnit,
        direction: Int,
    )

    fun stand(
        scope: RoomScope,
        unit: RoomUnit,
    )

    /** Lives only while the room is loaded: never saved. */
    @Suppress("LongParameterList") // who the bot is and where it stands
    fun addBot(
        scope: RoomScope,
        name: String,
        figure: String,
        female: Boolean,
        tile: Point,
        direction: Int,
        motto: String = "",
    ): RoomUnit

    fun removeBot(
        scope: RoomScope,
        unit: RoomUnit,
    )

    fun walkTo(
        scope: RoomScope,
        unit: RoomUnit,
        tile: Point,
    ): Boolean

    fun closeTiles(
        scope: RoomScope,
        tiles: Collection<Point>,
        closed: Boolean,
    )

    /** Clients report clicks on users ([zel.room.event.RoomEvent.ClickUser]) without a wired box asking for them. */
    fun userClicks(
        room: Room,
        on: Boolean,
    )

    /** Players get only the floor furni within [tiles] of their avatar; 0 shows the whole room. For large rooms. */
    fun viewDistance(
        room: Room,
        tiles: Int,
    )

    /** One player's own view distance in the room (a larger screen, a view zoomed out), until they leave. */
    fun viewDistance(
        scope: RoomScope,
        playerId: Int,
        tiles: Int,
    )

    /** The room's floor comes from [plan] when it loads, not from its stored map: for floors of any size. */
    fun floor(
        roomId: Int,
        plan: () -> FloorPlan?,
    )

    /** Floor pictures for one player only: no furni of the room, nothing to walk on or use. */
    fun showScenery(
        scope: RoomScope,
        playerId: Int,
        items: List<Scenery>,
    )

    fun hideScenery(
        scope: RoomScope,
        playerId: Int,
        ids: List<Int>,
    )

    fun say(
        scope: RoomScope,
        speaker: RoomUnit,
        text: String,
        shout: Boolean = false,
    )

    fun whisper(
        scope: RoomScope,
        speaker: RoomUnit,
        to: RoomUnit,
        text: String,
    )

    fun notify(
        scope: RoomScope,
        playerId: Int,
        type: String,
        params: Map<String, String>,
    )

    fun travel(
        scope: RoomScope,
        unit: RoomUnit,
        roomId: Int,
    ): Boolean
}

class Scenery(
    val id: Int,
    val spriteId: Int,
    val x: Int,
    val y: Int,
    val z: Double,
    val state: Int,
    val direction: Int = 0,
)

interface Moves {
    fun furni(
        furni: RoomFurni,
        tile: Point,
        z: Double,
        direction: Int = furni.direction,
        millis: Int = DEFAULT_MILLIS,
    ): Moves

    @Suppress("LongParameterList") // where the unit goes and how it faces
    fun unit(
        unit: RoomUnit,
        tile: Point,
        z: Double,
        direction: Int = unit.bodyDirection,
        millis: Int = DEFAULT_MILLIS,
    ): Moves

    fun send()

    companion object {
        const val DEFAULT_MILLIS = 500
    }
}
