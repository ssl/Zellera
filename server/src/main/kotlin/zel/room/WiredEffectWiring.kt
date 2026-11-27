package zel.room

import zel.game.permissions.Privilege
import zel.room.chat.ChatFilterResult
import zel.room.furni.FurniBehaviour
import zel.room.furni.behaviour.state.StatePart
import zel.room.games.GamesPart
import zel.room.unit.bot.BotDuties
import zel.wired.addon.Placeholders
import zel.wired.box.BoxType
import zel.wired.box.WiredBox
import zel.wired.effect.bot.BotEffectBoxes
import zel.wired.effect.bot.BotEffectRules
import zel.wired.effect.bot.BotErrands
import zel.wired.effect.bot.BotTalks
import zel.wired.effect.user.CopyAllowance
import zel.wired.effect.user.GameUserEffects
import zel.wired.effect.user.RewardLog
import zel.wired.effect.user.Teleports
import zel.wired.effect.user.UserEffectBoxes
import zel.wired.effect.user.UserEffectParts
import zel.wired.effect.user.UserEffectRules

internal class WiredEffectWiring(
    private val services: RoomEngineServices,
    core: CoreWiring,
    state: StatePart,
    games: GamesPart,
    wired: WiredEngineWiring,
    behaviourOf: (String) -> FurniBehaviour,
    travel: (RoomScope, Int, Int, Int, () -> Unit) -> Boolean,
) {
    val user =
        UserEffectBoxes(
            UserEffectParts(
                core.walking,
                state.hooks,
                games,
                wired.poster,
                GameUserEffects(
                    core.leaving,
                    core.life,
                    core.mutes,
                    core.rightsStore,
                    core.transitStore,
                    core.copies,
                    GameUserEffects.Services(
                        services.clients,
                        services.items,
                        services.persist,
                        behaviourOf,
                        travel,
                        { services.config.getInt(WIRED_TELEPORT_KEY, Teleports.LAND_MILLIS.toInt()).toLong() },
                        RewardLog(services.dataSource),
                        { services.config.getInt(CopyAllowance.MAX_LIVE_KEY, CopyAllowance.MAX_LIVE) },
                    ),
                ),
                core.unitsOnTiles,
                wired.wired,
                placeholders = { firing, _ -> Placeholders.values(firing) },
                variables = wired.variables.variables,
                roomLog = wired::roomLog,
            ),
        ).also { wired.userEffects.set(it.state) }
    val errands = BotErrands(core.units.bots, core.walking, state.hooks)
    val duties = BotDuties(core.units.bots, errands, state.hooks::holdItem, services.units)
    private val talks = BotTalks(wired.poster, user.texts)
    private val bots = BotEffectBoxes(core.units.bots, core.walking, errands, talks, user.teleports, core::playerName)
    val types: List<BoxType<WiredBox>> = user.types + bots.types
    val validations =
        UserEffectRules(
            superWired = ::superWired,
            filterText = ::filteredText,
            maxMessage = { services.config.getInt(SHOW_MESSAGE_LENGTH_KEY, UserEffectRules.MAX_MESSAGE) },
            maxLines = { services.config.getInt(SHOW_MESSAGE_LINES_KEY, UserEffectRules.MAX_LINES) },
            badgeExists = services.badgeExists,
        ).validations +
            BotEffectRules(
                { services.config.getInt(WIRED_NAME_LENGTH_KEY, BotEffectRules.MAX_NAME) },
                { services.config.getInt(BOT_LINE_LENGTH_KEY, BotEffectRules.MAX_LINE) },
                ::filteredText,
            ).validations
    val parts: List<RoomPart> = listOf(user.state, errands, talks)

    fun superWired(playerId: Int): Boolean =
        services.clients.onlinePlayer(playerId)?.hasPermission(Privilege.WIRED_STAFF) == true

    /** [text] through the chat filter unless [playerId] may skip it; empty when the filter blocks it. */
    fun filteredText(
        playerId: Int,
        text: String,
    ): String {
        val staff = services.clients.onlinePlayer(playerId)?.hasPermission(Privilege.CHAT_UNFILTERED) == true
        if (staff) return text
        return (services.chat.filter(playerId, text) as? ChatFilterResult.Say)?.text.orEmpty()
    }

    private companion object {
        const val WIRED_TELEPORT_KEY = "wired.teleport.duration.ms"
        const val WIRED_NAME_LENGTH_KEY = "wired.bot_name.max.characters"
        const val BOT_LINE_LENGTH_KEY = "wired.bot_line.max.characters"
        const val SHOW_MESSAGE_LENGTH_KEY = "wired.show_message.max.characters"
        const val SHOW_MESSAGE_LINES_KEY = "wired.show_message.max.lines"
    }
}
