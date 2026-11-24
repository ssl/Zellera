package zel.game.players.inventory

import zel.db.SqlFailure
import zel.game.bots.Bot
import zel.game.bots.Bots
import zel.game.players.Player
import zel.server.GameServices
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class BotInventory(
    services: GameServices,
    player: Player,
) {
    private val byId = ConcurrentHashMap<Int, Bot>()

    init {
        val owner = player.profile.id
        try {
            services.database.queries.select(Bots.OWNED_SQL, owner).each { row ->
                val bot = Bot.read(row)
                byId[bot.id] = bot
            }
        } catch (cause: SqlFailure) {
            log.error("bots of player {} not loaded", owner, cause)
        }
    }

    val count: Int get() = byId.size

    val all: Collection<Bot> get() = byId.values

    operator fun get(id: Int): Bot? = byId[id]

    fun put(bot: Bot) {
        byId[bot.id] = bot
    }

    fun remove(bot: Bot) {
        byId.remove(bot.id)
    }

    fun dispose() = byId.clear()

    private companion object {
        val log: Logger = LoggerFactory.getLogger(BotInventory::class.java)
    }
}
