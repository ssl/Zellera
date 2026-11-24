package zel.room.games

import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.state.StateShow
import zel.util.MILLIS_PER_SECOND
import java.util.concurrent.ConcurrentHashMap

@Suppress("TooManyFunctions") // one per clock action
class Clocks(
    private val part: GamesPart,
) {
    /** Shared by all rooms; a furni's clock is only touched under its room's lock. */
    private val byFurni = ConcurrentHashMap<RoomFurni, GameClock>()

    private val context get() = part.context

    fun of(
        furni: RoomFurni,
        kind: ClockKind,
    ): GameClock =
        byFurni.getOrPut(furni) {
            GameClock.load(kind, furni.definition.params, furni.state).also { furni.state = it.stored() }
        }

    fun click(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
        playerId: Int,
        param: Int,
    ) {
        if (!context.hooks.hasRights(scope, playerId)) return
        when {
            clock.kind == ClockKind.Hourglass -> toggleHourglass(scope, furni, clock)
            param == SET_TIME -> setTimeByClick(scope, furni, clock)
            clock.running -> togglePause(scope, furni, clock)
            else -> startByClick(scope, furni, clock)
        }
        scope.raise(
            RoomEvent.StateChanged(
                furni.id,
                scope.room.units
                    .ofPlayer(playerId)
                    ?.index,
            ),
        )
    }

    fun startFromWired(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
    ) {
        if (clock.running && !clock.paused) return
        val wasPaused = clock.paused
        stopClock(scope, clock)
        if (wasPaused) fireEnded(scope, clock)
        newGames(scope, clock)
        resetForStart(clock)
        clock.running = true
        clock.paused = false
        show(scope, furni, clock)
        fireStarted(scope, clock)
        startCounting(scope, furni, clock, startDelay(clock))
    }

    fun control(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
        action: ClockAction,
    ) {
        when (action) {
            ClockAction.Start -> startByControl(scope, furni, clock)
            ClockAction.Stop -> stop(scope, furni, clock)
            ClockAction.Reset -> reset(scope, furni, clock)
            ClockAction.Pause -> pause(scope, furni, clock)
            ClockAction.Resume -> resume(scope, furni, clock)
        }
    }

    fun adjust(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
        operator: ClockAdjust,
        millis: Int,
    ) {
        clock.setMillis(
            when (operator) {
                ClockAdjust.Add -> clock.millis + millis
                ClockAdjust.Subtract -> clock.millis - millis
                ClockAdjust.Set -> millis
            },
        )
        if (!clock.running && !clock.paused) clock.setWhileStopped = true
        // A written time never fires wf_trg_clock_counter, only counting into it does.
        keepAndShow(scope, furni, clock)
    }

    fun placed(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
    ) {
        if (clock.baseSeconds < clock.steps[0]) clock.baseSeconds = clock.steps[0]
        clock.seconds = initialSeconds(clock)
        clock.halfSecond = false
        keepAndShow(scope, furni, clock)
    }

    fun pickedUp(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
    ) {
        stopClock(scope, clock)
        clock.ticking = false
        clock.chain++
        clock.seconds = initialSeconds(clock)
        clock.halfSecond = false
        furni.state = clock.stored()
        furni.dirty = true
        byFurni.remove(furni)
    }

    fun unload(
        scope: RoomScope,
        all: Collection<RoomFurni>,
    ) {
        all.forEach { furni ->
            val clock = byFurni.remove(furni) ?: return@forEach
            clock.running = false
            clock.paused = false
            clock.chain++
            if (clock.kind.countsUp) clock.setMillis(0) else clock.seconds = clock.baseSeconds
            furni.state = clock.stored()
            furni.dirty = true
        }
        endGames(scope)
    }

    fun endRunning(
        scope: RoomScope,
        all: Collection<RoomFurni>,
    ): Boolean {
        val running =
            all
                .mapNotNull { furni -> byFurni[furni]?.takeIf { it.kind.runsGames && it.running } }
        running.forEach { stopClock(scope, it) }
        if (running.isNotEmpty()) scope.raise(RoomEvent.GameEnded)
        return running.isNotEmpty()
    }

    fun runOf(all: Collection<RoomFurni>): ClockRun {
        val timers = all.mapNotNull { furni -> byFurni[furni]?.takeIf { it.kind.runsGames && it.running } }
        return when {
            timers.any { !it.paused } -> ClockRun.Running
            timers.isNotEmpty() -> ClockRun.Paused
            else -> ClockRun.Stopped
        }
    }

    private fun togglePause(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
    ) {
        clock.paused = !clock.paused
        if (clock.paused) {
            pauseGames(scope, clock)
        } else {
            resumeGames(scope, clock)
            startCounting(scope, furni, clock, resumeDelay(clock))
        }
    }

    private fun startByClick(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
    ) {
        clock.paused = false
        clock.running = true
        resetForStart(clock)
        show(scope, furni, clock)
        newGames(scope, clock)
        fireStarted(scope, clock)
        startCounting(scope, furni, clock, startDelay(clock))
    }

    private fun setTimeByClick(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
    ) {
        when {
            !clock.running -> {
                nextTime(scope, furni, clock)
            }

            clock.paused -> {
                stopClock(scope, clock)
                nextTime(scope, furni, clock)
                fireEnded(scope, clock)
            }
        }
    }

    private fun nextTime(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
    ) {
        if (clock.kind.countsUp) {
            clock.setMillis(0)
        } else {
            if (clock.seconds == clock.baseSeconds) {
                clock.baseSeconds = clock.steps.firstOrNull { clock.seconds < it } ?: clock.steps[0]
            }
            clock.seconds = clock.baseSeconds
        }
        keepAndShow(scope, furni, clock)
    }

    private fun toggleHourglass(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
    ) {
        if (clock.running && !clock.paused) {
            reset(scope, furni, clock)
        } else {
            stopClock(scope, clock)
            clock.setMillis(0)
            startTimer(scope, furni, clock)
            keepAndShow(scope, furni, clock)
        }
    }

    private fun startTimer(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
    ) {
        if (clock.running) return
        clock.running = true
        clock.paused = false
        clock.setWhileStopped = false
        if (shouldResetOnStart(clock)) {
            resetForStart(clock)
            show(scope, furni, clock)
        }
        newGames(scope, clock)
        fireStarted(scope, clock)
        startCounting(scope, furni, clock, startDelay(clock))
    }

    private fun startByControl(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
    ) {
        val active = clock.running || clock.paused
        if (active) {
            stopClock(scope, clock)
            fireEnded(scope, clock)
        }
        if (!active && clock.kind.countsUp && clock.kind != ClockKind.Hourglass && clock.setWhileStopped) {
            // A stopped counter an adjust clock set counts on from that time; after a plain stop it starts from 0
            // like a running one, handled below.
            startTimer(scope, furni, clock)
            return
        }
        if (clock.kind.countsUp) clock.setMillis(0) else clock.seconds = clock.baseSeconds
        if (clock.kind == ClockKind.Hourglass) {
            startTimer(scope, furni, clock)
            keepAndShow(scope, furni, clock)
        } else {
            keepAndShow(scope, furni, clock)
            startTimer(scope, furni, clock)
        }
    }

    private fun stop(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
    ) {
        val active = clock.running || clock.paused
        stopClock(scope, clock)
        keepAndShow(scope, furni, clock)
        if (active) fireEnded(scope, clock)
    }

    private fun reset(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
    ) {
        val active = clock.running || clock.paused
        stopClock(scope, clock)
        if (clock.kind.countsUp) clock.setMillis(0) else clock.seconds = clock.baseSeconds
        keepAndShow(scope, furni, clock)
        if (active) fireEnded(scope, clock)
    }

    private fun pause(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
    ) {
        if (!clock.running || clock.paused) return
        clock.paused = true
        pauseGames(scope, clock)
        keepAndShow(scope, furni, clock)
    }

    private fun resume(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
    ) {
        if (!clock.paused) return
        clock.paused = false
        resumeGames(scope, clock)
        startCounting(scope, furni, clock, resumeDelay(clock))
        keepAndShow(scope, furni, clock)
    }

    private fun startCounting(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
        delay: Long,
    ) {
        if (clock.ticking) return
        clock.ticking = true
        clock.chain++
        schedule(scope, furni, clock, delay)
    }

    private fun schedule(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
        delay: Long,
    ) {
        val chain = clock.chain
        val furniId = furni.id
        context.jobs.after(scope.room, delay) { later ->
            val current = later.room.furni[furniId]
            if (current != null && byFurni[current] === clock && chain == clock.chain) count(later, current, clock)
        }
    }

    private fun count(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
    ) {
        if (!clock.running || clock.paused) {
            clock.ticking = false
            return
        }
        if (clock.kind.countsUp) countUp(scope, furni, clock) else countDown(scope, furni, clock)
    }

    private fun countDown(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
    ) {
        clock.seconds--
        if (clock.seconds > 0) {
            show(scope, furni, clock)
            context.host.clockCounted(scope, furni, clock.millis)
            schedule(scope, furni, clock, MILLIS_PER_SECOND)
        } else {
            clock.ticking = false
            stopClock(scope, clock)
            show(scope, furni, clock)
            context.host.clockCounted(scope, furni, clock.millis)
            fireEnded(scope, clock)
        }
    }

    private fun countUp(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
    ) {
        clock.setMillis(clock.millis + clock.nextCountMillis)
        if (clock.kind != ClockKind.Hourglass) show(scope, furni, clock)
        context.host.clockCounted(scope, furni, clock.millis)
        if (clock.millis >= clock.maxMillis) {
            clock.ticking = false
            stopClock(scope, clock)
            keepAndShow(scope, furni, clock)
            fireEnded(scope, clock)
        } else {
            schedule(scope, furni, clock, clock.nextCountMillis.toLong())
        }
    }

    private fun stopClock(
        scope: RoomScope,
        clock: GameClock,
    ) {
        clock.running = false
        clock.paused = false
        if (clock.kind.runsGames) endGames(scope)
    }

    private fun endGames(scope: RoomScope) {
        part
            .of(scope.room)
            .fieldGames()
            .filter { it.run != GameRun.Idle }
            .forEach { it.endRound(scope) }
    }

    private fun newGames(
        scope: RoomScope,
        clock: GameClock,
    ) {
        if (clock.kind.runsGames) part.of(scope.room).startRound(scope)
    }

    private fun pauseGames(
        scope: RoomScope,
        clock: GameClock,
    ) {
        if (clock.kind.runsGames) part.of(scope.room).fieldGames().forEach { it.pause(scope) }
    }

    private fun resumeGames(
        scope: RoomScope,
        clock: GameClock,
    ) {
        if (clock.kind.runsGames) part.of(scope.room).fieldGames().forEach { it.resume(scope) }
    }

    private fun fireStarted(
        scope: RoomScope,
        clock: GameClock,
    ) {
        if (clock.kind.runsGames) scope.raise(RoomEvent.GameStarted)
    }

    private fun fireEnded(
        scope: RoomScope,
        clock: GameClock,
    ) {
        if (clock.kind.runsGames) scope.raise(RoomEvent.GameEnded)
    }

    private fun initialSeconds(clock: GameClock): Int = if (clock.kind.countsUp) 0 else clock.baseSeconds

    private fun shouldResetOnStart(clock: GameClock): Boolean =
        if (clock.kind.countsUp) clock.seconds >= clock.baseSeconds else clock.seconds <= 0

    private fun resetForStart(clock: GameClock) {
        if (clock.kind.countsUp) clock.setMillis(0) else clock.seconds = clock.baseSeconds
    }

    private fun startDelay(clock: GameClock): Long =
        if (clock.kind.countsUp) clock.nextCountMillis.toLong() else MILLIS_PER_SECOND

    private fun resumeDelay(clock: GameClock): Long = if (clock.kind.countsUp) clock.nextCountMillis.toLong() else 0

    private fun show(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
    ) {
        furni.state = clock.stored()
        if (clock.kind == ClockKind.Hourglass) {
            val shown = clock.shown()
            if (shown == clock.shownState) return
            clock.shownState = shown
        }
        val dirty = furni.dirty
        StateShow.data(scope, furni, clock.stored())
        furni.dirty = dirty
    }

    private fun keepAndShow(
        scope: RoomScope,
        furni: RoomFurni,
        clock: GameClock,
    ) {
        show(scope, furni, clock)
        furni.dirty = true
    }

    private companion object {
        const val SET_TIME = 2
    }
}
