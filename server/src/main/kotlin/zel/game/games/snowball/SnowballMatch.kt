@file:Suppress("MagicNumber") // wire variable indexes and Direction8 offsets

package zel.game.games.snowball

import zel.game.games.snowball.arena.ArenaDefinition
import zel.game.games.snowball.arena.ArenaDefinition.Tile
import zel.game.games.snowball.sim.ArenaStage
import zel.game.games.snowball.sim.ArenaTile
import zel.game.games.snowball.sim.Compass
import zel.game.games.snowball.sim.GameEvent
import zel.game.games.snowball.sim.GameObject
import zel.game.games.snowball.sim.GameObjectData
import zel.game.games.snowball.sim.Human
import zel.game.games.snowball.sim.Snowball
import zel.game.games.snowball.sim.SnowballGiver
import zel.game.games.snowball.sim.SnowballMachine
import zel.game.games.snowball.sim.SnowballPile
import zel.game.games.snowball.sim.StatEvent
import zel.game.games.snowball.sim.SyncArena
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs

/** The server side of one Snowball stage; requests queue from any thread and only [tick] simulates. */
class SnowballMatch(
    val arena: ArenaDefinition,
    val mode: SnowballMode,
    val players: List<Player>,
    val stageSeconds: Int,
) {
    data class Player(
        val userId: Int,
        val humanId: Int,
        val team: Int,
        val name: String,
        val motto: String,
        val figure: String,
        val gender: String,
    )

    data class Tick(
        val turn: Int,
        val checksum: Long,
        val events: List<GameEvent>,
    ) {
        fun subturns(): List<List<GameEvent>> = listOf(events, emptyList(), emptyList())
    }

    private sealed interface Request {
        val userId: Int
    }

    private data class Move(
        override val userId: Int,
        val x: Int,
        val y: Int,
    ) : Request

    private data class ThrowAtHuman(
        override val userId: Int,
        val targetObjectId: Int,
        val trajectory: Int,
    ) : Request

    private data class ThrowAtPosition(
        override val userId: Int,
        val x: Int,
        val y: Int,
        val trajectory: Int,
    ) : Request

    private data class MakeSnowball(
        override val userId: Int,
    ) : Request

    val stageTurns: Int = stageTurns(stageSeconds)
    val startObjects: List<GameObjectData> = startObjects(arena, mode, players)
    val sync: SyncArena =
        SyncArena(
            ArenaStage(arena.width, arena.height, arena.heightmapString(), arena.fuse),
            mode.numberOfTeams,
            mode.id,
        )

    private val stats: Map<Int, SnowballPlayerStats> = players.associate { it.userId to SnowballPlayerStats() }
    private val humans: Map<Int, Human>
    private val requests = ConcurrentLinkedQueue<Request>()
    private val leaves = ConcurrentLinkedQueue<Int>()
    private val left = HashSet<Int>()
    private val refillTimers = HashMap<Int, Int>()
    private val pickupCooldowns = HashMap<Int, Int>()
    private var nextSnowballId: Int

    init {
        sync.onStat = ::count
        sync.stage.createObjects(startObjects)
        val objects = sync.stage.objects
        humans = objects.filterIsInstance<Human>().associateBy { it.userId }
        nextSnowballId = (objects.maxOfOrNull { it.id } ?: 0) + 1
    }

    val turn: Int get() = sync.turn

    val isTimeUp: Boolean get() = sync.turn >= stageTurns

    val secondsLeft: Int get() = maxOf(0, stageSeconds - sync.turn * SyncArena.TURN_MILLIS / 1000)

    fun move(
        userId: Int,
        x: Int,
        y: Int,
    ) {
        requests += Move(userId, x, y)
    }

    fun throwAtHuman(
        userId: Int,
        targetObjectId: Int,
        trajectory: Int,
    ) {
        requests += ThrowAtHuman(userId, targetObjectId, trajectory)
    }

    fun throwAtPosition(
        userId: Int,
        x: Int,
        y: Int,
        trajectory: Int,
    ) {
        requests += ThrowAtPosition(userId, x, y, trajectory)
    }

    fun makeSnowball(userId: Int) {
        requests += MakeSnowball(userId)
    }

    fun leave(userId: Int) {
        if (userId in stats) leaves += userId
    }

    fun hasLeft(userId: Int): Boolean = userId in left

    fun human(userId: Int): Human? = humans[userId]

    fun score(userId: Int): Int = humans[userId]?.score ?: 0

    fun stats(userId: Int): SnowballPlayerStats? = stats[userId]

    fun teamScore(team: Int): Int = players.filter { it.team == team }.sumOf { score(it.userId) }

    fun snapshot(): List<GameObjectData> = sync.stage.snapshot()

    fun snapshotRebuildsExactly(): Boolean = sync.stage.snapshotRebuildsExactly()

    fun tick(): Tick {
        val turn = sync.turn
        val checksum = sync.simulateTurn()
        val events = buildEvents()
        for (event in events) sync.queue(turn + 1, 0, event)
        return Tick(turn, checksum, events)
    }

    private fun buildEvents(): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        for (leaver in generateSequence { leaves.poll() }) {
            val human = humans[leaver]
            if (left.add(leaver) && human != null && sync.stage.objectOrNull(human.id) === human) {
                events += GameEvent.HumanLeftGame(human)
            }
        }

        val moves = HashMap<Int, Move>()
        val actions = HashMap<Int, Request>()
        for (request in generateSequence { requests.poll() }.filter { it.userId !in left }) {
            if (request is Move) moves[request.userId] = request else actions[request.userId] = request
        }

        val objects = sync.stage.objects
        val playing = objects.filterIsInstance<Human>().filter { isPlaying(it) }
        for (human in playing) {
            moves[human.userId]?.takeIf { insideArena(it.x, it.y) }?.let {
                events +=
                    GameEvent.NewMoveTarget(human, it.x, it.y)
            }
            when (val action = actions[human.userId]) {
                is ThrowAtHuman -> throwAtHuman(human, action, events)
                is ThrowAtPosition -> throwAtPosition(human, action, events)
                is MakeSnowball -> if (human.canMakeSnowballs()) events += GameEvent.HumanStartsToMakeASnowball(human)
                else -> Unit
            }
        }

        val available = refillMachines(objects, events)
        for (human in playing.filter { mayPickUp(it) }) {
            val giver = available.keys.firstOrNull { available.getValue(it) > 0 && onPickupTile(human, it) } ?: continue
            available[giver] = available.getValue(giver) - 1
            events += GameEvent.HumanGetsSnowballsFromMachine(human, giver)
            pickupCooldowns[human.id] = PICKUP_COOLDOWN_TURNS
        }
        pickupCooldowns.replaceAll { _, turns -> maxOf(0, turns - 1) }
        return events
    }

    /** The snowballs each active giver can hand out this turn, machines refilled first. */
    private fun refillMachines(
        objects: List<GameObject>,
        events: MutableList<GameEvent>,
    ): LinkedHashMap<SnowballGiver, Int> {
        val available = LinkedHashMap<SnowballGiver, Int>()
        for (obj in objects) {
            when {
                obj is SnowballMachine && obj.isActive -> available[obj] = refill(obj, events)
                obj is SnowballPile && obj.isActive -> available[obj] = obj.snowballs
            }
        }
        return available
    }

    private fun refill(
        machine: SnowballMachine,
        events: MutableList<GameEvent>,
    ): Int {
        if (machine.snowballs >= machine.maxSnowballs) {
            refillTimers[machine.id] = 0
            return machine.snowballs
        }
        val turns = (refillTimers[machine.id] ?: 0) + 1
        if (turns < REFILL_TURNS) {
            refillTimers[machine.id] = turns
            return machine.snowballs
        }
        refillTimers[machine.id] = 0
        events += GameEvent.MachineCreatesSnowball(machine)
        return machine.snowballs + 1
    }

    private fun isPlaying(human: Human): Boolean = human.isActive && human.userId in stats && human.userId !in left

    private fun insideArena(
        x: Int,
        y: Int,
    ): Boolean = x in 0..(arena.width - 1) * ArenaTile.WORLD_SIZE && y in 0..(arena.height - 1) * ArenaTile.WORLD_SIZE

    private fun throwAtHuman(
        human: Human,
        request: ThrowAtHuman,
        events: MutableList<GameEvent>,
    ) {
        if (!validThrow(human, request.trajectory)) return
        val target = sync.stage.objectOrNull(request.targetObjectId) as? Human ?: return
        if (target === human || !isPlaying(target) || (target.team == human.team && !mode.isDeathmatch) ||
            isOwnTile(human, target.x, target.y)
        ) {
            return
        }
        events += GameEvent.HumanThrowsSnowballAtHuman(human, target, request.trajectory)
        events += GameEvent.CreateSnowball(nextSnowballId++, human, target.x, target.y, request.trajectory)
    }

    private fun throwAtPosition(
        human: Human,
        request: ThrowAtPosition,
        events: MutableList<GameEvent>,
    ) {
        if (!validThrow(human, request.trajectory) || !insideArena(request.x, request.y) ||
            isOwnTile(human, request.x, request.y)
        ) {
            return
        }
        events += GameEvent.HumanThrowsSnowballAtPosition(human, request.x, request.y, request.trajectory)
        events += GameEvent.CreateSnowball(nextSnowballId++, human, request.x, request.y, request.trajectory)
    }

    private fun mayPickUp(human: Human): Boolean =
        human.isStandingStill &&
            human.canMove() &&
            human.snowballs < Human.MAX_SNOWBALLS &&
            (pickupCooldowns[human.id] ?: 0) == 0

    private fun count(event: StatEvent) {
        when (event) {
            is StatEvent.Hit -> {
                stats[event.thrower.userId]?.let {
                    it.snowballHits++
                    if (event.knockdown) it.kills++
                }
                stats[event.target.userId]?.let {
                    it.snowballHitsTaken++
                    if (event.knockdown) it.deaths++
                }
            }

            is StatEvent.FriendlyHit -> {
                stats[event.thrower.userId]?.let { it.friendlyHits++ }
            }

            is StatEvent.Thrown -> {
                stats[event.thrower.userId]?.let { it.snowballsThrown++ }
            }

            is StatEvent.Made -> {
                stats[event.human.userId]?.let { it.snowballsCreated++ }
            }

            is StatEvent.PickedUp -> {
                stats[event.human.userId]?.let { it.snowballsFromMachine++ }
            }

            is StatEvent.TreeHit -> {
                stats[event.thrower.userId]?.let { it.treesHit++ }
            }
        }
    }

    companion object {
        const val REFILL_TURNS = 34

        const val PICKUP_COOLDOWN_TURNS = 7

        internal const val ARENA_CENTRE_TILE = 25

        private val DIR8_X = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
        private val DIR8_Y = intArrayOf(-1, -1, 0, 1, 1, 1, 0, -1)

        fun stageTurns(stageSeconds: Int): Int =
            (stageSeconds * 1000 + SyncArena.TURN_MILLIS - 1) / SyncArena.TURN_MILLIS

        internal fun startObjects(
            arena: ArenaDefinition,
            mode: SnowballMode,
            players: List<Player>,
        ): List<GameObjectData> {
            val objects = mutableListOf<GameObjectData>()
            val taken = HashSet<Tile>()

            fun add(
                ref: Int,
                vars: (id: Int, fuse: ArenaDefinition.FuseObject) -> IntArray,
            ) {
                val fuse = checkNotNull(arena.fuseObject(ref)) { "fuse $ref is missing" }
                objects += GameObjectData(vars(objects.size + 1, fuse), emptyList())
                taken += Tile(fuse.x, fuse.y)
            }
            for (tree in arena.trees) {
                add(tree.fuse) { id, f ->
                    intArrayOf(
                        GameObject.TYPE_TREE,
                        id,
                        world(f.x),
                        world(f.y),
                        f.dir,
                        f.height,
                        f.id,
                        tree.maxHits,
                        tree.hits,
                    )
                }
            }
            for (pile in arena.piles) {
                add(pile.fuse) { id, f ->
                    intArrayOf(GameObject.TYPE_PILE, id, world(f.x), world(f.y), pile.max, pile.count, f.id)
                }
            }
            for (machine in arena.machines) {
                add(machine.fuse) { id, f ->
                    intArrayOf(
                        GameObject.TYPE_MACHINE,
                        id,
                        world(f.x),
                        world(f.y),
                        f.dir,
                        machine.max,
                        machine.count,
                        f.id,
                    )
                }
            }
            val spawner = Spawner(arena, mode, taken)
            for (player in players) {
                objects += humanObject(player, spawner.next(player.team))
            }
            return objects
        }

        /** Hands out each team's spawn tiles in order, skipping taken ones; any free walkable tile after that. */
        private class Spawner(
            private val arena: ArenaDefinition,
            private val mode: SnowballMode,
            private val taken: MutableSet<Tile>,
        ) {
            private val nextSpawn = HashMap<Int, Int>()
            private val stage by lazy { ArenaStage(arena.width, arena.height, arena.heightmapString(), arena.fuse) }

            fun next(team: Int): Tile {
                val spawns = spawns(arena, mode, team)
                var index = nextSpawn[team] ?: 0
                var spawn: Tile? = null
                while (index < spawns.size && spawn == null) {
                    spawn = spawns[index++].takeIf { it !in taken }
                }
                nextSpawn[team] = index
                return (spawn ?: firstFreeTile()).also { taken += it }
            }

            private fun firstFreeTile(): Tile =
                (0 until stage.height)
                    .asSequence()
                    .flatMap { y -> (0 until stage.width).asSequence().map { x -> Tile(x, y) } }
                    .firstOrNull { stage.tile(it.x, it.y)?.walkable() == true && it !in taken }
                    ?: error("No free tile left for a Snowball player")
        }

        private fun humanObject(
            player: Player,
            spawn: Tile,
        ): GameObjectData {
            val body = Compass.octantOf(Compass.headingOf(ARENA_CENTRE_TILE - spawn.x, ARENA_CENTRE_TILE - spawn.y))
            val x = world(spawn.x)
            val y = world(spawn.y)
            return GameObjectData(
                intArrayOf(
                    GameObject.TYPE_HUMAN,
                    player.humanId,
                    x,
                    y,
                    spawn.x,
                    spawn.y,
                    body,
                    Human.MAX_HIT_POINTS,
                    Human.MAX_SNOWBALLS,
                    0,
                    0,
                    Human.STATE_NORMAL,
                    spawn.x,
                    spawn.y,
                    x,
                    y,
                    0,
                    player.team,
                    player.userId,
                ),
                listOf(player.name, player.motto, player.figure, player.gender),
            )
        }

        /** A team's spawn tiles; deathmatch interleaves both teams' tiles. */
        internal fun spawns(
            arena: ArenaDefinition,
            mode: SnowballMode,
            team: Int,
        ): List<Tile> {
            if (!mode.isDeathmatch) return arena.spawns(team)
            val first = arena.spawns(1)
            val second = arena.spawns(2)
            return (
                0 until
                    maxOf(
                        first.size,
                        second.size,
                    )
            ).flatMap { listOfNotNull(first.getOrNull(it), second.getOrNull(it)) }
        }

        private fun world(tile: Int): Int = tile * ArenaTile.WORLD_SIZE

        private fun isOwnTile(
            human: Human,
            x: Int,
            y: Int,
        ): Boolean {
            val tile = human.nextTile ?: human.currentTile
            return tile.x == ArenaStage.worldToTile(x) && tile.y == ArenaStage.worldToTile(y)
        }

        private fun validThrow(
            human: Human,
            trajectory: Int,
        ): Boolean = human.canThrowSnowballs() && trajectory in Snowball.TRAJECTORY_QUICK..Snowball.TRAJECTORY_DEFAULT

        internal fun onPickupTile(
            human: Human,
            giver: SnowballGiver,
        ): Boolean {
            val at = human.currentTile
            val dx = at.x - giver.tile.x
            val dy = at.y - giver.tile.y
            if (giver is SnowballMachine) {
                val front = (giver.direction8 + 4) and 7
                return dx == DIR8_X[front] && dy == DIR8_Y[front]
            }
            return abs(dx) + abs(dy) == 1
        }
    }
}
