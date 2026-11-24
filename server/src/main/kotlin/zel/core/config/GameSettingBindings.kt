package zel.core.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.content.ContentDirs
import zel.core.Configuration
import zel.game.bots.BotLimits
import zel.game.pets.PetNameLimits
import zel.game.players.figure.LookValidation
import zel.game.players.inventory.Inventory
import zel.game.players.subscriptions.ClubSettings
import zel.protocol.typed.PacketDispatcher
import java.nio.file.Path

internal class GameSettingBindings(
    private val configuration: Configuration,
    private val installedFiguredata: () -> Path? = { ContentDirs.fromEnvironment().figuredata },
) {
    private class Binding(
        val key: String,
        val apply: (String) -> Unit,
    )

    private fun int(key: String) = configuration.getInt(key)

    private fun text(key: String) = configuration.getValue(key)

    private fun bind(
        key: String,
        apply: (String) -> Unit,
    ) = Binding(key, apply)

    private val rooms =
        listOf(
            bind("bots.speech.min_gap.seconds") { BotLimits.minChatDelay = int(it) },
            bind("bots.name.max.characters") { BotLimits.maxNameLength = int(it) },
            bind("bots.speech.max_gap.seconds") { BotLimits.maxChatDelay = int(it) },
            bind("player.inventory.max_furni") { Inventory.maximumItems = int(it) },
            bind("player.inventory.max_bots") { BotLimits.maxInventory = int(it) },
            bind("pets.name.min.characters") { PetNameLimits.minimum = int(it) },
            bind("pets.name.max.characters") { PetNameLimits.maximum = int(it) },
        )

    private val club =
        listOf(
            bind("club.payday.day_of_month") {
                val day = configuration.getInt(it, 1)
                payday { copy(dayOfMonth = day) }
            },
            bind("club.payday.currency") { payday { copy(currency = text(it)) } },
            bind("club.payday.kickback.percent") {
                val percent = configuration.getInt(it, ClubSettings.DEFAULT_KICKBACK_PERCENT)
                payday { copy(kickbackPercent = percent) }
            },
            bind("club.payday.streak.rewards") {
                val text = configuration.getValue(it, ClubSettings.DEFAULT_STREAK_REWARDS)
                val rewards = ClubSettings.streakRewards(text)
                payday { copy(streakRewards = rewards) }
            },
            bind("club.payday.spending_restarts_with_membership") {
                val fromStart = configuration.getBoolean(it, false)
                payday { copy(spendingFromStart = fromStart) }
            },
            bind("club.renewal_discount.is_on") {
                ClubSettings.discount = ClubSettings.discount.copy(enabled = configuration.getBoolean(it, false))
            },
            bind("club.renewal_discount.before_end.days") {
                val days = configuration.getInt(it, ClubSettings.DEFAULT_DISCOUNT_DAYS)
                ClubSettings.discount = ClubSettings.discount.copy(daysBeforeEnd = days)
            },
        )

    private val clothing = listOf(bind("player.figure", ::applyClothingValidation))

    private val packets =
        listOf(
            bind("server.log.shows_packets") { PacketDispatcher.showPackets = configuration.getBoolean(it) },
        )

    fun applyAll() = (rooms + club + clothing + packets).forEach(::attempt)

    @Suppress("TooGenericExceptionCaught") // one bad setting must not stop the rest
    private fun attempt(binding: Binding) {
        try {
            binding.apply(binding.key)
        } catch (e: RuntimeException) {
            log.error("setting {} not applied, its previous value stays: {}", binding.key, e.message)
        }
    }

    private fun payday(change: ClubSettings.Payday.() -> ClubSettings.Payday) {
        ClubSettings.payday = ClubSettings.payday.change()
    }

    private fun applyClothingValidation(key: String) {
        fun checked(moment: String) = configuration.getBoolean("$key.checked_$moment", true)

        LookValidation.configure(
            LookValidation.Settings(
                onClubExpired = checked("when_club_ends"),
                onLogin = checked("at_login"),
                onChangeLooks = checked("on_change"),
                onFootballGate = checked("at_football_gate"),
                clubItemsFree = configuration.getBoolean("$key.club_items_are_free", true),
                ownedItemsOnly = configuration.getBoolean("$key.owned_items_only", false),
            ),
            figuredataUrl(),
        )
    }

    internal fun figuredataUrl(): String =
        text("player.figure.data_url").ifEmpty { installedFiguredata()?.toUri()?.toString().orEmpty() }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GameSettingBindings::class.java)
    }
}
