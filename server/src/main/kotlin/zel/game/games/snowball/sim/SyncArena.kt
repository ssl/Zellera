package zel.game.games.snowball.sim

/** The turn clock of one Snowball stage: 3 subturns of 50 ms make a 150 ms turn, one pulse is one subturn. */
class SyncArena(
    tiles: ArenaStage,
    numberOfTeams: Int,
    val mode: Int = if (numberOfTeams == 1) MODE_DEATHMATCH else MODE_TEAM,
) {
    init {
        require(mode == MODE_TEAM || mode == MODE_DEATHMATCH) { "Snowball mode $mode" }
    }

    val stage: SyncStage = SyncStage(this, tiles)
    private val queues = HashMap<Int, List<MutableList<GameEvent>>>()
    private val checksums = HashMap<Int, Long>()
    var turn: Int = 0
        private set
    var subturn: Int = 0
        private set
    private var replayingTurn = false

    val isDeathMatch: Boolean get() = mode == MODE_DEATHMATCH

    var onStat: (StatEvent) -> Unit
        get() = stage.onStat
        set(value) {
            stage.onStat = value
        }

    fun queue(
        turn: Int,
        subturn: Int,
        event: GameEvent,
    ) {
        require(subturn in 0 until SUBTURNS_PER_TURN) { "subturn $subturn" }
        queues.getOrPut(turn) { newQueue() }[subturn].add(event)
    }

    private fun newQueue(): List<MutableList<GameEvent>> = List(SUBTURNS_PER_TURN) { mutableListOf() }

    fun pulse() {
        queues[turn]?.let { lists ->
            val events = lists[subturn]
            while (events.isNotEmpty()) {
                events.removeAt(0).apply(stage)
            }
        }
        if (!replayingTurn) {
            stage.subturn()
        }
        if (subturn >= SUBTURNS_PER_TURN - 1) {
            checksums[turn] = stage.checksum(turn)
            queues.remove(turn)
            turn++
            replayingTurn = false
        }
        subturn++
        if (subturn >= SUBTURNS_PER_TURN) {
            subturn = 0
        }
    }

    fun simulateTurn(): Long {
        val simulated = turn
        repeat(SUBTURNS_PER_TURN) { pulse() }
        return checkNotNull(checksums[simulated]) { "no checksum for turn $simulated" }
    }

    fun checksum(turn: Int): Long? = checksums[turn]

    /** Seeks to [turn] without stepping, as a client does after a full status. */
    internal fun seekToTurn(
        turn: Int,
        checksum: Long,
    ) {
        this.turn = turn
        subturn = 0
        checksums[turn] = checksum
        queues.clear()
        queues[turn] = newQueue()
        replayingTurn = true
    }

    companion object {
        const val SUBTURNS_PER_TURN = 3
        const val SUBTURN_MILLIS = 50
        const val TURN_MILLIS = SUBTURNS_PER_TURN * SUBTURN_MILLIS

        const val MODE_TEAM = 0
        const val MODE_DEATHMATCH = 1
    }
}
