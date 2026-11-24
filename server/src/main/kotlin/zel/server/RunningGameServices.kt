package zel.server

import zel.core.CommandLog
import zel.core.Configuration
import zel.core.Texts
import zel.core.threading.GameThreads
import zel.db.Database
import zel.game.Game
import zel.game.GameCore
import zel.game.guilds.BadgeImager
import zel.game.players.ChatStyles
import zel.game.players.Player
import zel.game.players.inventory.BadgeCaches
import zel.net.clients.ClientConnections
import zel.room.chat.ChatBubbleStyles
import java.util.Random
import java.util.concurrent.ThreadLocalRandom

class RunningGameServices(
    private val core: GameCore,
    override val config: Configuration,
    override val threading: GameThreads,
    override val game: Game,
) : GameServices {
    override val texts: Texts get() = core.texts
    override val database: Database get() = core.database
    override val badgeImager: BadgeImager get() = checkNotNull(core.badgeImager())
    override val commandLog: CommandLog get() = core.commandLog
    override val clients: ClientConnections get() = checkNotNull(core.clients())
    override val badgeCaches: BadgeCaches get() = game.badgeCaches
    override val chatStyles: ChatStyles get() = game.chatStyles
    override val chatBubbles: ChatBubbleStyles get() = game.chatBubbles
    override val isShuttingDown: Boolean get() = core.state.isShuttingDown

    override fun unixTimestamp(): Int = core.now()

    override fun onlinePlayer(id: Int): Player? = core.clients()?.onlinePlayer(id)

    override fun random(): Random = ThreadLocalRandom.current()
}
