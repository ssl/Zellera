package zel.server

import zel.core.CommandLog
import zel.core.Configuration
import zel.core.Texts
import zel.core.threading.GameThreads
import zel.db.Database
import zel.game.Game
import zel.game.guilds.BadgeImager
import zel.game.players.ChatStyles
import zel.game.players.Player
import zel.game.players.inventory.BadgeCaches
import zel.net.clients.ClientConnections
import zel.room.chat.ChatBubbleStyles
import java.util.Random

interface GameServices {
    val config: Configuration
    val texts: Texts
    val database: Database
    val game: Game
    val threading: GameThreads
    val badgeImager: BadgeImager
    val commandLog: CommandLog
    val clients: ClientConnections
    val badgeCaches: BadgeCaches
    val chatStyles: ChatStyles
    val chatBubbles: ChatBubbleStyles

    val isShuttingDown: Boolean

    fun unixTimestamp(): Int

    fun onlinePlayer(id: Int): Player?

    /** The calling thread's random source. */
    fun random(): Random
}
