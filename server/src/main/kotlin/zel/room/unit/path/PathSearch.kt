package zel.room.unit.path

import zel.room.map.Point
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.ceil

/**
 * A best-first walk search. A step costs 10, a diagonal one 11, every started 0.1 of height change 3 more, unless the
 * change is 0.2 or less; a tile is ranked by its cost so far plus 9 per straight tile left, and a cheaper way to a
 * waiting tile ranks it again. Of equal ranks the tile found first goes first, and neighbours are found in a fixed
 * order. A heuristic of 12 or a kept first rank would take dearer ways; the free 0.2 lets walks cross pressure plates.
 */
class PathSearch(
    private val maxExpanded: Int = DEFAULT_MAX_EXPANDED,
    private val stepLimits: () -> StepLimits = { StepLimits() },
) {
    data class StepLimits(
        val maxUp: Double = MAX_STEP_UP,
        val mayFall: Boolean = true,
    )

    fun find(
        grid: WalkGrid,
        from: Point,
        goal: Point,
        mover: Mover,
        fromZ: Double = grid.tile(from).height,
    ): PathResult {
        if (!mayEnd(grid, goal, mover)) return PathResult.GoalRefused
        if (from == goal) return PathResult.Found(emptyList())
        val query = Query(grid, from, goal, mover, fromZ)
        val path =
            query.search(unitsBlock = true)
                ?: if (grid.walkThrough) query.search(unitsBlock = false) else null
        return path?.let { PathResult.Found(it) } ?: PathResult.NoPath
    }

    fun mayEnd(
        grid: WalkGrid,
        goal: Point,
        mover: Mover,
    ): Boolean {
        val state = grid.tile(goal).state
        val open =
            when (state) {
                TileState.Open, is TileState.Seat, is TileState.Bed -> true
                TileState.Void, TileState.Blocked -> false
                is TileState.Restricted -> mover.mayUse(state.passage, asGoal = true)
            }
        return open && (!grid.holdsUnit(goal, mover) || grid.nearDoor(goal) || state.isTransit())
    }

    /**
     * One search; tiles are plain indices so a step makes no objects. On a large floor the search stays inside a
     * window around the walk: its notes then cost the window, not the floor.
     */
    private inner class Query(
        val grid: WalkGrid,
        val from: Point,
        val goal: Point,
        val mover: Mover,
        val fromZ: Double,
    ) {
        private val whole = grid.floor.tileCount <= WHOLE_FLOOR
        private val left = if (whole) 0 else (minOf(from.x, goal.x) - REACH).coerceAtLeast(0)
        private val top = if (whole) 0 else (minOf(from.y, goal.y) - REACH).coerceAtLeast(0)
        private val width =
            if (whole) grid.floor.width else (maxOf(from.x, goal.x) + REACH + 1).coerceAtMost(grid.floor.width) - left
        private val height =
            if (whole) grid.floor.height else (maxOf(from.y, goal.y) + REACH + 1).coerceAtMost(grid.floor.height) - top
        private val goalIsBed = grid.tile(goal).state is TileState.Bed
        private val limits = stepLimits()
        private val neighbours = if (grid.tile(from).state is TileState.Bed) OUT_OF_BED else NEIGHBOURS
        private val fromIndex = index(from.x, from.y)
        private val goalIndex = index(goal.x, goal.y)
        private lateinit var marks: Marks

        fun search(unitsBlock: Boolean): List<Point>? {
            if (width * height > WINDOW_LIMIT) return null
            marks = Marks.take(width * height)
            try {
                for (tile in grid.unitTiles()) {
                    if (inside(tile.x, tile.y) && grid.holdsUnit(tile, mover)) marks.unit(index(tile.x, tile.y))
                }
                return run(unitsBlock)
            } finally {
                marks.wipe()
            }
        }

        private fun run(unitsBlock: Boolean): List<Point>? {
            val cost = marks.cost
            val cameFrom = marks.cameFrom
            val settled = marks.settled
            val open = PriorityQueue(ORDER)
            var found = 0L
            marks.touch(fromIndex)
            cost[fromIndex] = 0
            open.add(Node(fromIndex, rank(0, from.x, from.y), found++, 0))
            var expanded = 0
            while (open.isNotEmpty() && expanded < maxExpanded) {
                val node = open.poll()
                val at = node.tile
                // A tile ranked again leaves its older, dearer entry behind.
                if (settled[at] || node.cost != cost[at]) continue
                if (at == goalIndex) return walkBack(cameFrom)
                settled[at] = true
                expanded++
                val x = at % width + left
                val y = at / width + top
                for (direction in neighbours) {
                    val nx = x + DX[direction]
                    val ny = y + DY[direction]
                    val step = stepCost(x, y, nx, ny, unitsBlock)
                    if (step == NO_STEP) continue
                    val costThere = cost[at] + step
                    val i = index(nx, ny)
                    if (settled[i] || costThere >= cost[i]) continue
                    open.add(Node(i, rank(costThere, nx, ny), found++, costThere))
                    if (cost[i] == UNKNOWN) marks.touch(i)
                    cost[i] = costThere
                    cameFrom[i] = at
                }
            }
            return null
        }

        private fun walkBack(cameFrom: IntArray): List<Point> {
            val path = ArrayList<Point>()
            var at = goalIndex
            while (at != fromIndex) {
                path += Point(at % width + left, at / width + top)
                at = cameFrom[at]
            }
            return path.asReversed()
        }

        private fun rank(
            costSoFar: Int,
            x: Int,
            y: Int,
        ): Int = costSoFar + TILE_LEFT * (abs(x - goal.x) + abs(y - goal.y))

        private fun inside(
            x: Int,
            y: Int,
        ): Boolean = x - left in 0 until width && y - top in 0 until height

        private fun index(
            x: Int,
            y: Int,
        ): Int = (y - top) * width + (x - left)

        private fun tile(
            x: Int,
            y: Int,
        ): WalkTile {
            val i = index(x, y)
            return marks.tiles[i] ?: grid.tile(Point(x, y)).also { marks.keep(i, it) }
        }

        @Suppress("ReturnCount") // one early way out per reason a step is refused
        private fun stepCost(
            x: Int,
            y: Int,
            nx: Int,
            ny: Int,
            unitsBlock: Boolean,
        ): Int {
            if (!inside(nx, ny)) return NO_STEP
            val after = index(nx, ny)
            val toGoal = after == goalIndex
            if (!toGoal && !mayPass(nx, ny, unitsBlock)) return NO_STEP
            val diagonal = x != nx && y != ny
            if (diagonal && !mayCut(x, y, nx, ny)) return NO_STEP
            val heightBefore = if (index(x, y) == fromIndex) fromZ else tile(x, y).height
            val rise = tile(nx, ny).height - heightBefore
            if (rise > limits.maxUp + EPSILON && !(toGoal && goalIsBed)) return NO_STEP
            if (!limits.mayFall && -rise > limits.maxUp + EPSILON) return NO_STEP
            val climb = if (isJumped(x, y) || isJumped(nx, ny)) 0.0 else abs(rise)
            val tenths = if (climb <= FREE_CHANGE + EPSILON) 0 else ceil(climb * TENTHS - EPSILON).toInt()
            return (if (diagonal) DIAGONAL else STRAIGHT) + HEIGHT_COST * tenths
        }

        /** A horse jumps an obstacle and does not climb it: the furni's own height costs no step. */
        private fun isJumped(
            x: Int,
            y: Int,
        ): Boolean = inside(x, y) && (tile(x, y).state as? TileState.Restricted)?.passage is Passage.Obstacle

        private fun mayPass(
            x: Int,
            y: Int,
            unitsBlock: Boolean,
        ): Boolean = isOpen(x, y) && !(unitsBlock && marks.units[index(x, y)] && !grid.nearDoor(x, y))

        private fun isOpen(
            x: Int,
            y: Int,
        ): Boolean =
            when (val state = tile(x, y).state) {
                TileState.Open -> true
                is TileState.Restricted -> mover.mayUse(state.passage, asGoal = false)
                TileState.Void, TileState.Blocked, is TileState.Seat, is TileState.Bed -> false
            }

        private fun mayCut(
            x: Int,
            y: Int,
            nx: Int,
            ny: Int,
        ): Boolean {
            if (!inside(x, ny) || !inside(nx, y)) return false
            return isOpen(x, ny) || isOpen(nx, y)
        }
    }

    /** A search's notes per tile, kept per thread: a search clears only the tiles it touched, not the whole map. */
    private class Marks(
        size: Int,
    ) {
        val cost = IntArray(size) { UNKNOWN }
        val cameFrom = IntArray(size) { -1 }
        val settled = BooleanArray(size)

        /** Tiles a unit that blocks this mover stands on. */
        val units = BooleanArray(size)

        /** The walk tiles this search looked up. */
        val tiles = arrayOfNulls<WalkTile>(size)
        private var touched = IntArray(INITIAL_TOUCHED)
        private var count = 0
        private var kept = IntArray(INITIAL_TOUCHED)
        private var keptCount = 0
        private var taken = false

        fun touch(index: Int) {
            if (count == touched.size) touched = touched.copyOf(count * 2)
            touched[count++] = index
        }

        fun unit(index: Int) {
            units[index] = true
            touch(index)
        }

        fun keep(
            index: Int,
            tile: WalkTile,
        ) {
            tiles[index] = tile
            if (keptCount == kept.size) kept = kept.copyOf(keptCount * 2)
            kept[keptCount++] = index
        }

        fun wipe() {
            for (k in 0 until count) {
                val i = touched[k]
                cost[i] = UNKNOWN
                cameFrom[i] = -1
                settled[i] = false
                units[i] = false
            }
            for (k in 0 until keptCount) tiles[kept[k]] = null
            count = 0
            keptCount = 0
            taken = false
        }

        companion object {
            private const val INITIAL_TOUCHED = 1024
            private val perThread = ThreadLocal<Marks>()

            /** The thread's notes for a map of [size] tiles; fresh ones while a search already holds them. */
            fun take(size: Int): Marks {
                val own = perThread.get()
                if (own != null && own.cost.size >= size && !own.taken) return own.also { it.taken = true }
                return Marks(size).also {
                    it.taken = true
                    if (own == null || !own.taken) perThread.set(it)
                }
            }
        }
    }

    private class Node(
        val tile: Int,
        val rank: Int,
        val found: Long,
        val cost: Int,
    )

    companion object {
        const val MAX_STEP_UP = 1.5

        const val DEFAULT_MAX_EXPANDED = 20_000

        /** Floors up to this many tiles are searched whole; larger ones within [REACH] tiles around the walk. */
        private const val WHOLE_FLOOR = 1 shl 18
        private const val REACH = 96
        private const val WINDOW_LIMIT = 1 shl 21

        private const val STRAIGHT = 10
        private const val DIAGONAL = 11
        private const val TILE_LEFT = 9
        private const val HEIGHT_COST = 3
        private const val FREE_CHANGE = 0.2
        private const val TENTHS = 10.0
        private const val UNKNOWN = Int.MAX_VALUE
        private const val EPSILON = 1e-4
        private const val NO_STEP = -1
        private val DX = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
        private val DY = intArrayOf(-1, -1, 0, 1, 1, 1, 0, -1)

        // West, north, south, east, then the diagonals; a walk out of a bed tries north-east before south-west.
        private val NEIGHBOURS = intArrayOf(6, 0, 4, 2, 7, 5, 1, 3)
        private val OUT_OF_BED = intArrayOf(6, 0, 4, 2, 7, 1, 5, 3)

        private val ORDER: Comparator<Node> =
            Comparator { a, b ->
                val byRank = a.rank.compareTo(b.rank)
                if (byRank != 0) byRank else a.found.compareTo(b.found)
            }
    }
}
