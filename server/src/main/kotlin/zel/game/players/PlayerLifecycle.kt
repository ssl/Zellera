package zel.game.players

import zel.ext.ExtensionBus
import zel.ext.ServerEvent
import zel.game.quests.PlayerActions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/** The save opens before unregistering, so a new login waits for it rather than read stale rows. */
internal class PlayerLifecycle(
    private val player: Player,
) {
    private val gone = AtomicBoolean()

    fun connect(): Boolean {
        val gate = LoginGate(player)
        if (!gate.passes()) return false
        with(player.profile) {
            isOnline = true
            lastIp = gate.address
        }
        player.services.game.accountLevels
            .recountLogged(player.profile.id)
            ?.let { player.accountLevel = it.level }
        player.messenger.connectionChanged(player, online = true, inRoom = false)
        return true
    }

    fun saveProfile() = player.profile.run()

    fun disconnect() {
        if (gone.getAndSet(true)) return
        val id = player.profile.id
        val game = player.services.game
        val players = checkNotNull(game.players) { "the players are not started" }
        val save = players.beginDisconnectSave(id)
        try {
            synchronized(player) {
                game.rooms.leftGame(id)
                players.unregister(player)
            }
            player.profile.isOnline = false
            player.messenger.connectionChanged(player, online = false, inRoom = false)
            player.messenger.dispose()
            PlayerActions.forget(id)
            game.rewardTrackService?.unload(id)
            game.snowball?.lobby?.userLoggedOut(id)
            ExtensionBus.event(ServerEvent.LoggedOut(id))
        } finally {
            save.queue { durableSaves().forEach { (what, write) -> guarded(what, write) } }
        }
    }

    private fun durableSaves(): List<Pair<String, () -> Unit>> =
        listOf(
            "profile" to { player.profile.run() },
            "inventory" to { player.inventory.dispose() },
            "achievements" to {
                player.services.game.achievements
                    ?.playerLeft(player)
            },
            "stats" to { player.stats.dispose() },
        )

    private fun guarded(
        what: String,
        write: () -> Unit,
    ) {
        try {
            write()
        } catch (
            // one failing part must not stop the rest of the logout saves
            @Suppress("TooGenericExceptionCaught") failure: Exception,
        ) {
            log.error("The {} of player {} could not be saved at logout", what, player.profile.id, failure)
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(PlayerLifecycle::class.java)
    }
}
