package zel.room.entry

import zel.protocol.generated.FloorHeightMapOut
import zel.protocol.generated.FurnitureLabelsAndRegionsOut
import zel.protocol.generated.NotificationDialogOut
import zel.protocol.generated.WiredEnvironmentOut
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.room.unit.RoomUnit

@Suppress("TooManyFunctions") // one function per part of the entry burst a room part changes
interface EntryViews {
    fun configurationStates(
        scope: RoomScope,
        playerId: Int,
    ): List<Boolean> = List(CONFIGURATION_STATES) { false }

    /** The furni of [furni] the room's viewers are sent: an invisible furni controller that is on keeps some back. */
    fun shownFurni(
        scope: RoomScope,
        furni: Collection<RoomFurni>,
    ): Collection<RoomFurni> = furni

    /** The floor tiles the entrant's HeightMap gives no height (0xFFFF): those an area hide that is on hides. */
    fun hiddenTiles(scope: RoomScope): Set<Point> = emptySet()

    /**
     * The area list of the entrant's FloorHeightMap: one entry per area hide that is on; the client hides their floor
     * while it builds the room.
     */
    fun areaHides(scope: RoomScope): List<FloorHeightMapOut.List1Item> = emptyList()

    fun wiredEnvironment(scope: RoomScope): WiredEnvironmentOut =
        WiredEnvironmentOut(false, WiredEnvironmentOut.Trailing1(emptyList()))

    fun labels(
        scope: RoomScope,
        level: Int,
    ): FurnitureLabelsAndRegionsOut = FurnitureLabelsAndRegionsOut(emptyList())

    fun wiredPermissions(
        scope: RoomScope,
        level: Int,
    ): Pair<Boolean, Boolean> = (level >= DoorCheck.OWNER).let { it to it }

    /** Notices the entrant gets right after RoomEntryInfo. */
    fun entryNotices(
        scope: RoomScope,
        level: Int,
    ): List<NotificationDialogOut> = emptyList()

    fun allMuted(scope: RoomScope): Boolean = false

    fun otherUnits(
        scope: RoomScope,
        playerId: Int,
    ): Boolean = false

    fun effect(unit: RoomUnit): Int = 0

    /** Room state the viewer gets right after the furni lists (Objects, Items), before RoomEntryInfo. */
    fun afterFurni(
        scope: RoomScope,
        playerId: Int,
    ) {}

    fun afterBurst(
        scope: RoomScope,
        unit: RoomUnit,
        playerId: Int,
    ) {}

    /** The player came in through the linked furni [furniId] from another room. */
    fun arrivedThrough(
        scope: RoomScope,
        unit: RoomUnit,
        furniId: Int,
    ) {}

    companion object {
        const val CONFIGURATION_STATES = 5

        val None: EntryViews = object : EntryViews {}
    }
}
