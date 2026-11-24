package zel.core

import zel.core.threading.GameThreads
import zel.game.Game
import zel.game.achievements.AchievementEvents
import zel.game.players.Player
import zel.util.MILLIS_PER_MINUTE
import java.time.Instant

class OnlineMinutes(
    private val threading: GameThreads,
    private val state: ServerState,
    private val game: () -> Game?,
) {
    fun start() {
        threading.run(::minute, MILLIS_PER_MINUTE)
    }

    private fun minute() {
        try {
            if (!state.isReady) return
            val online = game()?.players?.all ?: return
            credit(online, Instant.now().epochSecond.toInt())
        } finally {
            start()
        }
    }

    private fun credit(
        online: Collection<Player>,
        now: Int,
    ) {
        val inRoom = AchievementEvents.roomTime()
        val present = AchievementEvents.gamePresence()
        online.forEach {
            inRoom.minute(it, now)
            present.minute(it, now)
        }
        val ids = online.map { it.profile.id }
        inRoom.keepOnly(ids)
        present.keepOnly(ids)
    }
}
