package zel.room.games.freeze

import zel.game.achievements.GameEvents
import zel.game.effects.EffectRoles
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.games.FieldFamily
import zel.room.games.FieldGame
import zel.room.games.FreezeRules
import zel.room.games.GameContext
import zel.room.games.GameOut
import zel.room.games.GameRun
import zel.room.games.GamesPart
import zel.room.games.RecordOnlyScope
import zel.room.games.RoomGames
import zel.room.games.TeamColor
import zel.room.games.TeamMember
import zel.room.games.TeamScoreboard
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import kotlin.math.abs

@Suppress("TooManyFunctions") // the rules of the game in one place
class FreezeGame(
    context: GameContext,
    games: RoomGames,
    private val part: GamesPart,
) : FieldGame(context, games, FieldFamily.Freeze) {
    private val rules: FreezeRules get() = context.rules.freeze

    private val pendingEndStates = mutableListOf<RoomFurni>()

    override val scoreName = "FreezeGame"

    override fun member(
        unit: RoomUnit,
        playerId: Int,
        color: TeamColor,
    ): TeamMember = FreezeMember(unit, playerId, color)

    override fun effectOf(
        scope: RoomScope,
        member: TeamMember,
    ): Int = (member as? FreezeMember)?.effect() ?: 0

    private fun equip(
        scope: RoomScope,
        member: TeamMember,
    ) {
        val tile =
            scope.room.furni
                .all()
                .firstOrNull { it.behaviour is FreezeTile && it.definition.params.isNotBlank() }
        (member as? FreezeMember)?.kit =
            FreezeKit.of(member.color, gateEffect(scope, member.color), tile?.definition?.params)
    }

    override fun joined(
        scope: RoomScope,
        member: TeamMember,
    ) {
        equip(scope, member)
        member.joinedAt(runningMillis(scope))
        if (run != GameRun.Idle) member.startWith(START_POINTS)
    }

    override fun leave(
        scope: RoomScope,
        member: TeamMember,
    ) {
        part.release(member.unit)
        super.leave(scope, member)
    }

    fun begin(scope: RoomScope) {
        if (run != GameRun.Idle) return
        members().forEach { equip(scope, it) }
        startRound(scope)
        sendOffField(scope)
        members().forEach {
            it.startWith(START_POINTS)
            it.joinedAt(0)
            GameOut.playerValue(scope, it, it.lives)
        }
        GameOut.states(scope, startStates(scope))
        scheduleCycle(scope, round)
    }

    private fun startStates(scope: RoomScope): List<RoomFurni> {
        val all = scope.room.furni.all()
        val exits = all.filter { it.behaviour is FreezeExit }.onEach { it.state = "1" }
        val field = all.filter { it.behaviour is FreezeBlock || it.behaviour is FreezeTile }.onEach { it.state = "0" }
        val boards =
            all.filter { furni ->
                val board = furni.behaviour as? TeamScoreboard
                if (board?.family == FieldFamily.Freeze) furni.state = score(board.color).toString()
                board?.family == FieldFamily.Freeze
            }
        return exits + field + boards
    }

    private fun sendOffField(scope: RoomScope) {
        val room = scope.room
        if (room.furni.all().none { it.behaviour is FreezeExit }) return
        room.units
            .all()
            .filter { unit ->
                val playerId = (unit.kind as? UnitKind.Player)?.playerId
                playerId != null && memberOf(playerId) == null &&
                    room.furni
                        .at(unit.tile)
                        .furni
                        .any { it.behaviour is FreezeTile }
            }.forEach { unit ->
                context.hooks.giveEffect(scope, unit, EffectRoles.of(EffectRoles.TELEPORT), null)
                moveToExit(scope, unit)
            }
    }

    private fun moveToExit(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        val exits =
            scope.room.furni
                .all()
                .filter { it.behaviour is FreezeExit }
        val exit = exits.randomOrNull(context.random)?.anchor ?: return
        context.jobs.after(scope.room, EXIT_TELEPORT_DELAY_MS) { later ->
            if (later.room.units[unit.index] === unit) {
                context.walking.place(later, unit, exit)
                context.walking.sendNow(later, listOf(unit))
            }
        }
    }

    private fun scheduleCycle(
        scope: RoomScope,
        ofRound: Int,
    ) {
        context.jobs.after(scope.room, CYCLE_MS) { later ->
            if (run != GameRun.Idle && round == ofRound) {
                members().forEach { cycle(later, it) }
                if (run == GameRun.Running) GameOut.states(later, changedScoreboards(later))
                if (run != GameRun.Idle) scheduleCycle(later, ofRound)
            }
        }
    }

    private fun cycle(
        scope: RoomScope,
        member: FreezeMember,
    ) {
        when (member.cycle()) {
            FreezeMember.Tick.Nothing -> {}

            FreezeMember.Tick.EffectChanged -> {
                context.hooks.giveEffect(scope, member.unit, member.effect(), null)
            }

            FreezeMember.Tick.Thawed -> {
                part.release(member.unit)
                context.hooks.giveEffect(scope, member.unit, member.effect(), null)
            }

            FreezeMember.Tick.Out -> {
                playerOut(scope, member)
            }
        }
    }

    fun throwBall(
        scope: RoomScope,
        playerId: Int,
        tile: RoomFurni,
    ) {
        val member = memberOf(playerId) as? FreezeMember ?: return
        val at = tile.anchor ?: return
        if (run != GameRun.Running || !member.canThrow || !near(member.unit.tile, at)) return
        if (tile.state == BALL_STATE) return
        val shape = member.throwBall()
        tile.state = BALL_STATE
        GameOut.states(scope, listOf(tile))
        val ofRound = round
        val tileId = tile.id
        context.jobs.after(scope.room, EXPLOSION_DELAY_MS) { later ->
            val thrown = later.room.furni[tileId]
            if (thrown != null && round == ofRound && run != GameRun.Idle) {
                explode(later, member, thrown, shape)
            }
        }
    }

    private fun explode(
        scope: RoomScope,
        thrower: FreezeMember,
        tile: RoomFurni,
        shape: BallShape,
    ) {
        val center = tile.anchor ?: return
        val reached = FreezeRays.reached(scope.room, center, shape)
        val changed = mutableListOf<RoomFurni>()
        val frozen = mutableListOf<FreezeMember>()
        reached.forEach { (at, distance) ->
            scope.room.furni.at(at).furni.forEach { furni ->
                if (explodeFurni(scope, furni, thrower, distance)) changed += furni
            }
            members().filter { it.unit.tile == at && it.canBeFrozen }.forEach { victim ->
                freeze(scope, victim, thrower, distance)
                frozen += victim
            }
        }
        thrower.addBall(rules)
        GameOut.states(scope, changed + changedScoreboards(scope))
        frozen.forEach {
            context.hooks.giveEffect(RecordOnlyScope(scope), it.unit, it.kit.frozenEffect, null)
            GameOut.delayedEffect(scope, it.unit.index, it.kit.frozenEffect, it.effectDelay)
        }
    }

    private fun explodeFurni(
        scope: RoomScope,
        furni: RoomFurni,
        thrower: FreezeMember,
        distance: Int,
    ): Boolean =
        when (furni.behaviour) {
            is FreezeTile -> {
                furni.state = (EXPLODED + maxOf(1, distance) * DISTANCE_UNIT).toString()
                true
            }

            is FreezeBlock -> {
                val unbroken = furni.state.isEmpty() || furni.state == "0"
                if (unbroken) {
                    furni.state = brokenState(maxOf(1, distance))
                    addScore(scope, thrower, rules.blockPoints)
                }
                unbroken
            }

            else -> {
                false
            }
        }

    /** Thousands: 1 for an empty block, a power-up's kind for one that holds it; hundreds: tiles from the ball. */
    private fun brokenState(distance: Int): String {
        val random = context.random
        val holds = random.nextInt(PERCENT) < rules.powerUpChance
        val kind = if (holds) FIRST_POWER_UP + random.nextInt(POWER_UP_KINDS) else EMPTY_BLOCK
        return (kind * POWER_UP_UNIT + distance * DISTANCE_UNIT).toString()
    }

    private fun freeze(
        scope: RoomScope,
        victim: FreezeMember,
        thrower: FreezeMember,
        distance: Int,
    ) {
        victim.freeze(distance, rules)
        GameOut.playerValue(scope, victim, victim.lives)
        addScore(scope, victim, -rules.frozenLosePoints)
        if (victim.color != thrower.color) thrower.frozeOpponents++
        part.hold(victim.unit)
        victim.unit.path = emptyList()
    }

    /** False when [member] has no use for it and leaves it on the block. */
    fun takes(
        member: FreezeMember,
        powerUp: Int,
    ): Boolean = powerUp != POWER_UP_LIFE || member.mayTakeLife(rules)

    fun givePowerUp(
        scope: RoomScope,
        member: FreezeMember,
        powerUp: Int,
    ) {
        addScore(scope, member, rules.powerUpPoints)
        when (powerUp) {
            POWER_UP_REACH -> {
                member.reachFurther()
            }

            POWER_UP_BALL -> {
                member.addBall(rules)
            }

            POWER_UP_DIAGONAL -> {
                member.burstNext(FreezeMember.Burst.Diagonal)
            }

            POWER_UP_STAR -> {
                member.burstNext(FreezeMember.Burst.Star)
            }

            POWER_UP_LIFE -> {
                if (member.addLife(rules)) GameOut.playerValue(scope, member, member.lives)
            }

            POWER_UP_SHIELD -> {
                member.addProtection(rules)
                context.hooks.giveEffect(scope, member.unit, member.effect(), null)
            }
        }
    }

    private fun playerOut(
        scope: RoomScope,
        member: FreezeMember,
    ) {
        val color = member.color
        member.out = true
        part.release(member.unit)
        team(color)?.leave(member)
        games.playing.remove(member.playerId)
        GameOut.playing(scope, member.playerId, false)
        context.hooks.giveEffect(scope, member.unit, EffectRoles.of(EffectRoles.TELEPORT), null)
        moveToExit(scope, member.unit)
        val unit = member.unit
        context.jobs.after(scope.room, EXIT_CLEAR_EFFECT_MS) { later ->
            if (later.room.units[unit.index] === unit) context.hooks.giveEffect(later, unit, 0, null)
        }
        val changed = teamGates(scope, color) + changedScoreboards(scope, color)
        if (teams.count { it.members.isNotEmpty() } <= 1) {
            pendingEndStates += changed
            if (!part.endRunningClocks(scope)) endRound(scope)
        } else {
            GameOut.states(scope, changed)
        }
    }

    override fun endRound(scope: RoomScope) {
        recordEnd(scope)
        stopRunningTime(scope)
        val millis = runningMillis(scope)
        run = GameRun.Idle
        val winner = winningTeam()
        winner?.members?.forEach { GameOut.wave(scope, it.unit.index) }
        GameOut.states(scope, endStates(scope))
        thaw(scope)
        endAchievements(scope, winner?.color, millis)
    }

    private fun thaw(scope: RoomScope) {
        members().filter { it.isFrozen }.forEach {
            it.thaw()
            part.release(it.unit)
            context.hooks.giveEffect(scope, it.unit, it.effect(), null)
        }
    }

    private fun endStates(scope: RoomScope): List<RoomFurni> {
        val all = scope.room.furni.all()
        val exits = all.filter { it.behaviour is FreezeExit }.onEach { it.state = "0" }
        val pending = pendingEndStates.toList()
        pendingEndStates.clear()
        val closed = all.filter { it.behaviour is FreezeBlock && FreezeBlock.closeAtEnd(it) }
        return exits + pending + closed
    }

    private fun endAchievements(
        scope: RoomScope,
        winner: TeamColor?,
        millis: Long,
    ) {
        val hooks = context.hooks
        val players = members()
        val seconds = players.map { it.secondsPlayed(millis) }
        val total = experience(seconds)
        players.forEachIndexed { k, member ->
            if (member.color == winner && total > 0) {
                hooks.progress(member.playerId, GameEvents.GAME_PLAYER_XP, total)
                hooks.progress(member.playerId, GameEvents.GAME_FREEZE_WIN, total)
            }
            if (seconds[k] > 0) hooks.progress(member.playerId, GameEvents.GAME_FREEZE_PLAY_SECONDS, seconds[k])
            val froze = member.frozeOpponents
            if (froze > 0) hooks.progress(member.playerId, GameEvents.GAME_FREEZE_FROZE_OPPONENT, froze)
            member.frozeOpponents = 0
        }
        if (total > 0) hooks.progress(scope.room.info.ownerId, GameEvents.GAME_AUTHOR_XP, total)
    }

    private fun members(): List<FreezeMember> = teams.flatMap { it.members }.filterIsInstance<FreezeMember>()

    private fun near(
        a: Point,
        b: Point,
    ): Boolean = abs(a.x - b.x) <= 1 && abs(a.y - b.y) <= 1

    companion object {
        const val START_POINTS = 40
        const val BALL_REACH = 2
        const val BALL_STATE = "1000"
        const val POWER_UP_UNIT = 1000
        const val FIRST_POWER_UP = 2
        const val LAST_POWER_UP = 7

        private const val EXPLODED = 11_000
        private const val DISTANCE_UNIT = 100
        private const val EMPTY_BLOCK = 1
        private const val EXPLOSION_DELAY_MS = 2000L
        private const val CYCLE_MS = 1000L
        private const val EXIT_TELEPORT_DELAY_MS = 1500L
        private const val EXIT_CLEAR_EFFECT_MS = 3500L
        private const val PERCENT = 100
        private const val POWER_UP_KINDS = LAST_POWER_UP - FIRST_POWER_UP + 1
        private const val POWER_UP_REACH = 2
        private const val POWER_UP_BALL = 3
        private const val POWER_UP_DIAGONAL = 4
        private const val POWER_UP_STAR = 5
        private const val POWER_UP_LIFE = 6
        private const val POWER_UP_SHIELD = 7
    }
}
