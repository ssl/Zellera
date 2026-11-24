package zel.room.unit.path

import zel.room.Room
import zel.room.furni.FurniKind
import zel.room.furni.RoomFurni
import zel.room.furni.TileStack
import zel.room.map.Point
import kotlin.math.abs

object WalkTiles {
    private const val EPSILON = 1e-6

    private val DECIDER_ORDER: Comparator<RoomFurni> =
        compareBy<RoomFurni> { TileStack.level(it.top) }
            .thenBy { !it.definition.canWalk }
            .then(TileStack.TOP_ORDER)

    private const val GATE = "gate.plain"
    private val ALWAYS_OPEN = setOf("gate.club", "gate.group", "gate.effect")
    private val TRANSIT = setOf("transit.door", "gate.oneway")

    fun at(
        room: Room,
        tile: Point,
    ): WalkTile = of(room, room.furni.at(tile), room.floor.floorZ(tile), tile)

    /**
     * The z a user entering by the door appears at on [tile]: the tile's walk height, except that a deciding furni
     * that cannot be stood on (not walkable, no seat, no bed) counts at its own z, not its top.
     * Walkable furni counts at its top; a seat on the door counts as a seat.
     */
    fun entryHeight(
        room: Room,
        tile: Point,
    ): Double {
        val stack = room.furni.at(tile)
        val walk = of(room, stack, room.floor.floorZ(tile), tile)
        if (stack.walkTileZ() != null) return walk.height
        val decider = decider(stack, stack.helperZ()) ?: return walk.height
        val standable = decider.definition.canWalk || decider.definition.canSit || decider.definition.canLay
        return if (!standable && abs(walk.height - decider.top) <= EPSILON) decider.z else walk.height
    }

    private fun of(
        room: Room,
        stack: TileStack,
        floor: Double?,
        tile: Point,
    ): WalkTile {
        if (floor == null) return WalkTile.VOID
        if (tile in room.closedTiles) return WalkTile(TileState.Blocked, floor)
        if (stack.furni.isEmpty()) return WalkTile(TileState.Open, floor)
        val walkZ = stack.walkTileZ()
        val helperZ = stack.helperZ()
        val decider = decider(stack, helperZ)
        val bed = bed(stack)
        val state =
            when {
                bed != null -> TileState.Bed(bed)
                walkZ != null -> walkSeat(stack, walkZ)?.let { TileState.Seat(it) } ?: TileState.Open
                decider != null -> stateOf(room, decider, tile).closedUnder(stack, helperZ, decider)
                helperZ != null -> TileState.Blocked
                else -> TileState.Open
            }
        val height =
            walkZ ?: when (state) {
                is TileState.Seat -> state.furni.z
                is TileState.Bed -> state.furni.z
                else -> decider?.top ?: helperZ ?: floor
            }
        return WalkTile(state, height)
    }

    /** A seat at the walk tile's own z: the tile is a walk's goal, never a middle step; walks route round it. */
    private fun walkSeat(
        stack: TileStack,
        walkZ: Double,
    ): RoomFurni? =
        stack.furni
            .filter { !it.isHeightSetter() && it.definition.canSit && abs(it.z - walkZ) <= EPSILON }
            .maxWithOrNull(TileStack.TOP_ORDER)

    /**
     * The furni that decides the tile. Without a helper: the top item, where of furni with one top (single precision)
     * one nothing stands on decides whatever its z, so it blocks the tile; the height map's tie rule, except that the
     * z does not matter here.
     */
    private fun decider(
        stack: TileStack,
        helperZ: Double?,
    ): RoomFurni? {
        if (helperZ == null) return stack.furni.filter { !it.isHeightSetter() }.maxWithOrNull(DECIDER_ORDER)
        return stack.furni
            .filter { !it.isHeightSetter() && it.z >= helperZ - EPSILON && it.top > helperZ + EPSILON }
            .maxWithOrNull(compareBy<RoomFurni> { it.z }.thenBy { it.id })
    }

    /**
     * Over a helper, a walkable top does not open the tile when furni that cannot be stood on (not walkable, or a
     * seat) lie between the helper and that top: the tile stays blocked and walks go round it.
     */
    private fun TileState.closedUnder(
        stack: TileStack,
        helperZ: Double?,
        decider: RoomFurni,
    ): TileState {
        if (this != TileState.Open || helperZ == null) return this
        val closed =
            stack.furni.any {
                it !== decider && !it.isHeightSetter() && it.z >= helperZ - EPSILON && it.z < decider.z - EPSILON &&
                    (!it.definition.canWalk || it.definition.canSit)
            }
        return if (closed) TileState.Blocked else this
    }

    private fun stateOf(
        room: Room,
        furni: RoomFurni,
        tile: Point,
    ): TileState {
        val interaction = furni.interaction()
        val closing = furni.behaviour as? ClosedToWalks
        return when {
            closing?.closed(room, furni) == true -> {
                closing.passage(furni)?.let { TileState.Restricted(it) } ?: TileState.Blocked
            }

            interaction == GATE -> {
                if (furni.isOpenGate()) TileState.Open else TileState.Blocked
            }

            interaction in ALWAYS_OPEN -> {
                TileState.Open
            }

            interaction == "walk.rail" -> {
                TileState.Restricted(Passage.SkateRail)
            }

            interaction == "pet.obstacle" -> {
                TileState.Restricted(Passage.Obstacle(tile in ObstacleBar.middle(furni)))
            }

            interaction in TRANSIT && !furni.definition.canWalk -> {
                TileState.Restricted(Passage.Transit(furni.id))
            }

            interaction == "pet.nest.breeding" -> {
                TileState.Restricted(Passage.Nest(furni.id))
            }

            furni.definition.canSit -> {
                TileState.Seat(furni)
            }

            furni.definition.canWalk -> {
                TileState.Open
            }

            else -> {
                TileState.Blocked
            }
        }
    }

    private fun bed(stack: TileStack): RoomFurni? =
        stack.furni.filter { !it.isHeightSetter() && it.definition.canLay }.maxWithOrNull(TileStack.TOP_ORDER)

    private fun RoomFurni.isOpenGate(): Boolean = (state.trim().toIntOrNull() ?: 0) != 0

    private fun RoomFurni.interaction(): String? = definition.interaction.name

    private fun RoomFurni.isHeightSetter(): Boolean = kind == FurniKind.StackHelper || kind == FurniKind.WalkTile
}
