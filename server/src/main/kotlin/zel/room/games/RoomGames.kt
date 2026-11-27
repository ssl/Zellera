package zel.room.games

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.games.football.BallRun
import zel.room.games.freeze.FreezeGame
import zel.room.games.tag.TagGame
import zel.room.games.tag.TagKind
import zel.room.games.tileclaim.TileClaimGame
import zel.room.unit.RoomUnit

class RoomGames(
    private val context: GameContext,
    private val part: GamesPart,
) {
    val playing = HashMap<Int, RoomGame>()

    val steps = HashMap<Int, Int>()

    val gateLooks = HashMap<Int, GateLook>()

    var freeze: FreezeGame? = null
        private set

    var tileclaim: TileClaimGame? = null
        private set

    var wired: WiredTeams? = null
        private set

    val ballRuns = HashMap<Int, BallRun>()

    private val tags = HashMap<TagKind, TagGame>()

    private val fieldsPlaced = HashSet<Int>()

    private val shownAtTick = LinkedHashSet<RoomFurni>()

    private val atTick = ArrayList<(RoomScope) -> Unit>()

    /**
     * The game furni whose new state goes out with the room's next tick, in one ObjectsDataUpdate with the state they
     * have then, as the tick's first packet.
     */
    fun showAtTick(furni: Collection<RoomFurni>) {
        shownAtTick += furni
    }

    /** Runs [job] at the start of the room's next tick, before [showAtTick]'s furni go out. */
    fun atNextTick(job: (RoomScope) -> Unit) {
        atTick += job
    }

    fun tick(scope: RoomScope) {
        if (atTick.isNotEmpty()) {
            val due = atTick.toList()
            atTick.clear()
            due.forEach { it(scope) }
        }
        if (shownAtTick.isEmpty()) return
        val shown = shownAtTick.filter { scope.room.furni[it.id] === it }
        shownAtTick.clear()
        GameOut.states(scope, shown)
    }

    fun freezeOrNew(): FreezeGame = freeze ?: FreezeGame(context, this, part).also { freeze = it }

    fun tileclaimOrNew(): TileClaimGame = tileclaim ?: TileClaimGame(context, this, part).also { tileclaim = it }

    fun wiredOrNew(): WiredTeams = wired ?: WiredTeams(context, this).also { wired = it }

    fun tagOrNew(kind: TagKind): TagGame = tags.getOrPut(kind) { TagGame(context, this, kind) }

    fun firstFieldPlacement(furniId: Int): Boolean = fieldsPlaced.add(furniId)

    fun fieldGames(): List<RoomGame> = listOfNotNull(freeze, tileclaim)

    fun roomGame(): RoomGame? {
        val fields = fieldGames().filter { it.run != GameRun.Idle }
        return fields.firstOrNull { field -> field.teams.any { it.members.isNotEmpty() } }
            ?: wired
            ?: fields.firstOrNull()
    }

    fun teamOf(playerId: Int): TeamColor? = playing[playerId]?.memberOf(playerId)?.color

    /** The player's team in the room's game, the one team boxes ask (an idle field game's teams do not count). */
    fun roomTeamOf(playerId: Int): TeamColor? = roomGame()?.memberOf(playerId)?.color

    fun startRound(scope: RoomScope) {
        freezeOrNew().begin(scope)
        tileclaimOrNew().begin(scope)
        wiredOrNew().newRound(scope)
    }

    fun leave(
        scope: RoomScope,
        playerId: Int,
    ) {
        val game = playing[playerId] ?: return
        val member = game.memberOf(playerId)
        if (member != null) game.leave(scope, member) else playing.remove(playerId)
    }

    fun switchAway(
        scope: RoomScope,
        playerId: Int,
    ) {
        val game = playing[playerId] ?: return
        val member = game.memberOf(playerId)
        if (member != null) game.switchAway(scope, member) else playing.remove(playerId)
    }

    fun teamEffect(
        scope: RoomScope,
        unit: RoomUnit,
        effect: Int,
    ) = part.teamEffect(scope, unit, effect)

    fun unload(scope: RoomScope) {
        (fieldGames() + listOfNotNull(wired) + tags.values).forEach { it.clear(scope) }
    }
}
