package zel.game.players

import zel.room.chat.ChatBubbleStyle
import zel.room.chat.ChatBubbleStyles
import java.sql.ResultSet

/** Plain fields save with the stats, grouped ones on each change. */
class PlayerSettings internal constructor(
    private val store: PlayerStatsStore,
) {
    data class Volumes(
        val system: Int = 0,
        val furni: Int = 0,
        val trax: Int = 0,
    )

    data class ChatLayout(
        val mode: Int = 0,
        val bubbleWidth: Int = 0,
        val scrollSpeed: Int = 0,
    )

    data class WiredMenu(
        val menuButton: Boolean = false,
        val inspectButton: Boolean = false,
        val playtest: Boolean = false,
    )

    data class ModeratorWindow(
        val x: Int = 0,
        val y: Int = 0,
        val width: Int = 0,
        val height: Int = 0,
    )

    data class NavigatorWindow(
        val x: Int = 0,
        val y: Int = 0,
        val width: Int = 0,
        val height: Int = 0,
        val searchesOpen: Boolean = false,
        val resultsMode: Int = 0,
    ) {
        val isPlaced: Boolean get() = width > 0 && height > 0
    }

    internal enum class WiredColumn(
        val column: String,
    ) {
        WHISPER_OFF("wired_whisper_off"),
        MENU_BUTTON("wired_menu_button"),
        INSPECT_BUTTON("wired_inspect_button"),
        PLAYTEST("wired_playtest"),
    }

    var noFollowing = false
    var noFriendRequests = false
    var noRoomInvites = false
    var noCameraFollow = false
    var appearOffline = false
    var privateProfile = false
    var mutedPets = false
    var mutedBots = false
    var classicChat = false
    var bubble: ChatBubbleStyle = ChatBubbleStyles.NORMAL
    var volumes = Volumes()
    var clientFlags = 0
    var starterSearchesGiven = false

    var chatLayout = ChatLayout()
        private set
    var onlineIndicator = 0
        private set
    var wiredWhisperOff = false
        private set
    var wiredMenu = WiredMenu()
        private set
    var moderatorWindow = ModeratorWindow()
        private set
    var navigatorWindow = NavigatorWindow()
        private set
    var discord = DiscordPreferences.UNSET
        private set

    fun update(layout: ChatLayout) {
        chatLayout = layout
        store.saveChatLayout(layout)
    }

    fun update(window: ModeratorWindow) {
        moderatorWindow = window
        store.saveModeratorWindow(window)
    }

    fun update(window: NavigatorWindow) {
        navigatorWindow = window
        store.saveNavigatorWindow(window)
    }

    fun update(value: DiscordPreferences) {
        discord = value
        store.saveDiscord(value)
    }

    fun update(menu: WiredMenu) {
        val old = wiredMenu
        wiredMenu = menu
        if (old.menuButton != menu.menuButton) store.saveWiredFlag(WiredColumn.MENU_BUTTON, menu.menuButton)
        if (old.inspectButton != menu.inspectButton) store.saveWiredFlag(WiredColumn.INSPECT_BUTTON, menu.inspectButton)
        if (old.playtest != menu.playtest) store.saveWiredFlag(WiredColumn.PLAYTEST, menu.playtest)
    }

    fun savePreferences() = store.savePreferences(this)

    fun updateOnlineIndicator(value: Int) {
        onlineIndicator = value
        store.saveOnlineIndicator(value)
    }

    fun updateWiredWhisper(off: Boolean) {
        if (wiredWhisperOff == off) return
        wiredWhisperOff = off
        store.saveWiredFlag(WiredColumn.WHISPER_OFF, off)
    }

    internal fun load(
        row: ResultSet,
        bubbleOf: (Int) -> ChatBubbleStyle,
    ) {
        noFollowing = row.getBoolean("block_following")
        noFriendRequests = row.getBoolean("block_friend_requests")
        noRoomInvites = row.getBoolean("block_room_invites")
        noCameraFollow = row.getBoolean("block_camera_follow")
        appearOffline = row.getBoolean("hide_online")
        privateProfile = row.getBoolean("hide_profile")
        mutedBots = row.getBoolean("ignore_bots")
        mutedPets = row.getBoolean("ignore_pets")
        classicChat = row.getBoolean("old_chat")
        bubble = bubbleOf(row.getInt("chat_color"))
        volumes = Volumes(row.getInt("volume_system"), row.getInt("volume_furni"), row.getInt("volume_music"))
        clientFlags = row.getInt("ui_flags")
        starterSearchesGiven = row.getBoolean("saved_searches_seeded")
        chatLayout =
            ChatLayout(row.getInt("chat_mode"), row.getInt("chat_bubble_width"), row.getInt("chat_scroll_speed"))
        onlineIndicator = row.getInt("online_indicator")
        wiredWhisperOff = row.getBoolean("wired_whisper_off")
        wiredMenu =
            WiredMenu(
                row.getBoolean("wired_menu_button"),
                row.getBoolean("wired_inspect_button"),
                row.getBoolean("wired_playtest"),
            )
        moderatorWindow =
            ModeratorWindow(
                row.getInt("mod_window_x"),
                row.getInt("mod_window_y"),
                row.getInt("mod_window_width"),
                row.getInt("mod_window_height"),
            )
        navigatorWindow =
            NavigatorWindow(
                row.getInt("nav_window_x"),
                row.getInt("nav_window_y"),
                row.getInt("nav_window_width"),
                row.getInt("nav_window_height"),
                row.getBoolean("nav_window_searches_open"),
                row.getInt("nav_window_results_mode"),
            )
        discord = DiscordPreferences.stored(row.getInt("discord_version"), row.getInt("discord_sharing"))
    }
}
