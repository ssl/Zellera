package zel.game.players

import zel.game.icons.IconService
import zel.game.permissions.Privilege
import zel.game.players.inventory.Inventory
import zel.game.players.inventory.UnseenItems
import zel.net.clients.ClientConnection
import zel.protocol.outgoing.generic.alerts.MotdNotification
import zel.protocol.outgoing.users.RemainingMutePeriod
import zel.room.chat.ChatBubbleStyle
import zel.room.port.RoomPort
import zel.server.GameServices
import java.sql.ResultSet

val Player.connection: ClientConnection get() = checkNotNull(client) { "the player is disconnected" }

class Player(
    val services: GameServices,
    unseenStore: UnseenItems.Store,
    set: ResultSet,
    private val icons: IconService? = null,
) : Runnable {
    val profile = PlayerProfile(services, set)
    val stats: PlayerStats = PlayerStats.load(services, profile)
    val inventory =
        Inventory(
            services,
            unseenStore,
            this,
            services.game.catalogServices
                .required()
                .marketplace,
        )
    val messenger =
        services.game.messengerModule
            .required()
            .newFriendList()

    @Volatile
    var client: ClientConnection? = null

    /** The device this login came from; null with the device check off. */
    @Volatile
    var device: DeviceReading? = null

    @Volatile
    var accountLevel: Int = 1

    val wallet = PlayerWallet(this)

    init {
        messenger.load(this)
    }

    private val lifecycle = PlayerLifecycle(this)

    val iconService: IconService get() = checkNotNull(icons) { "the icon service is not started" }

    val rooms: RoomPort get() = services.game.rooms

    val inRoom: Boolean get() = rooms.roomOf(profile.id) != null

    val isOnline: Boolean get() = profile.isOnline

    fun connect(): Boolean = lifecycle.connect()

    fun disconnect() = lifecycle.disconnect()

    override fun run() = lifecycle.saveProfile()

    fun hasPermission(permission: Privilege): Boolean = hasPermission(permission.key)

    fun hasPermission(key: String): Boolean = services.game.permissions?.allows(this, key) ?: false

    fun whisper(message: String) = whisper(message, stats.settings.bubble)

    fun whisper(
        message: String,
        bubble: ChatBubbleStyle,
    ) = rooms.whisper(profile.id, message, bubble)

    fun alert(message: String) {
        client?.send(MotdNotification(listOf(message)))
    }

    /** Sends nothing until the next line; false for a player who is never muted. */
    fun mute(seconds: Int): Boolean {
        if (seconds <= 0 || hasPermission(Privilege.CHAT_MUTE_IMMUNE)) return false
        stats.standing.mute(seconds)
        stats.storeMute()
        return true
    }

    fun liftMute() {
        stats.standing.unmute()
        stats.storeMute()
        client?.send(RemainingMutePeriod(0))
    }
}
