package zel.game.achievements

import zel.content.ClientStrings
import zel.game.players.Player
import zel.room.listing.RoomCategory
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Minutes spent in a room count on the room's kind, once a stay has lasted over a minute. */
class RoomTimeAchievements(
    private val progress: AchievementProgress,
    private val categoryOf: (Int) -> RoomCategory?,
    private val clock: () -> Int,
) {
    enum class StayKind(
        val detail: String,
    ) {
        CHAT("chat"),
        TRADE("trade"),
        RPG("rpg"),
        GAMES("games"),
    }

    private val stays = ConcurrentHashMap<Int, Stay>()

    fun arrived(
        player: Player?,
        roomId: Int,
    ) {
        if (player == null || roomId <= 0) return
        stayOf(player).arrive(roomId, clock())
    }

    fun departed(player: Player?) {
        if (player != null) stays[player.profile.id]?.depart()
    }

    fun minute(
        player: Player?,
        now: Int,
    ) {
        if (player == null) return
        val rooms = player.rooms
        val roomId = rooms.roomOf(player.profile.id) ?: 0
        if (!stayOf(player).settled(roomId, now)) return
        val kind = kindOf(categoryOf(rooms.categoryOf(roomId))?.caption) ?: return
        progress.count(player, GameEvents.of(GameEvents.ROOM_TIME, kind.detail))
    }

    fun keepOnly(present: Collection<Int>) {
        val keep = present.toSet()
        stays.keys.removeIf { it !in keep }
    }

    private fun stayOf(player: Player) = stays.getOrPut(player.profile.id) { Stay() }

    internal class Stay {
        private var roomId = 0
        private var arrivedAt = 0

        @Synchronized
        fun arrive(
            roomId: Int,
            now: Int,
        ) {
            this.roomId = roomId
            arrivedAt = now
        }

        @Synchronized
        fun depart() {
            roomId = 0
        }

        @Synchronized
        fun settled(
            roomId: Int,
            now: Int,
        ): Boolean {
            if (this.roomId != roomId) {
                this.roomId = roomId
                arrivedAt = now
                return false
            }
            return roomId != 0 && now - arrivedAt > SECONDS_BEFORE_COUNTING
        }
    }

    companion object {
        internal const val SECONDS_BEFORE_COUNTING = 60

        private val BY_GLOBAL_KEY =
            mapOf(
                "CHAT" to StayKind.CHAT,
                "PARTY" to StayKind.CHAT,
                "OFFICIAL" to StayKind.CHAT,
                "TRADE" to StayKind.TRADE,
                "ROLEPLAY" to StayKind.RPG,
                "LIFE" to StayKind.RPG,
                "GAMES" to StayKind.GAMES,
            )

        private val BY_LOCAL_NAME =
            mapOf(
                "trading" to StayKind.TRADE,
                "ruilen" to StayKind.TRADE,
                "vaihtola" to StayKind.TRADE,
                "cambios" to StayKind.TRADE,
                "role playing" to StayKind.RPG,
                "agencies" to StayKind.RPG,
                "life" to StayKind.RPG,
            )

        /** The kind of room a category [label] names: by its global key, else by its local name. */
        fun kindOf(label: String?): StayKind? {
            if (label == null) return null
            val key = RoomCategory.globalKeyOf(label)
            if (key.isNotEmpty()) return BY_GLOBAL_KEY[key.uppercase(Locale.ROOT)]
            val plain = label.trim().lowercase(Locale.ROOT)
            return BY_LOCAL_NAME[plain] ?: StayKind.RPG.takeIf { plain in rolePlayNames() }
        }

        private fun rolePlayNames(): Set<String> =
            ClientStrings.withPrefix(ClientStrings.ROLE_PLAY_NAME_PREFIX).mapTo(HashSet()) { it.lowercase(Locale.ROOT) }
    }
}
