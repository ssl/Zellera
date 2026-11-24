package zel.core

import zel.core.threading.GameThreads
import zel.game.players.Players
import zel.game.quests.DailyTasks
import zel.game.quests.RoomPosture
import org.slf4j.LoggerFactory
import java.util.EnumSet

class DailyTaskPulseUpdater(
    private val tasks: DailyTasks,
    private val threading: GameThreads,
    private val state: ServerState,
    private val players: () -> Players?,
) : Runnable {
    fun start() {
        threading.run(this, PULSE_MS)
    }

    override fun run() {
        try {
            if (state.isReady) pulse()
        } finally {
            threading.run(this, PULSE_MS)
        }
    }

    // One player's failure must not stop the others.
    @Suppress("TooGenericExceptionCaught")
    private fun pulse() {
        val online = players()?.all ?: return
        online.forEach { player ->
            try {
                if (player.rooms.isSitting(player.profile.id)) {
                    tasks.countPulse(player, EnumSet.of(RoomPosture.SIT))
                }
            } catch (e: Exception) {
                log.warn("sit pulse of player {} not counted", player.profile.id, e)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DailyTaskPulseUpdater::class.java)
        private const val PULSE_MS = DailyTasks.PULSE_SECONDS * 1000L
    }
}
