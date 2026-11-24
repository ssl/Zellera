package zel.game.achievements

import zel.core.rules.rules
import zel.game.achievements.resolution.ResolutionRows
import zel.game.achievements.resolution.Resolutions
import zel.game.players.ChatStyles
import zel.game.players.Players
import zel.game.players.inventory.BadgeCaches
import zel.game.players.level.AccountLevels
import zel.protocol.achievements.AchievementMessages
import zel.protocol.achievements.AchievementPackets
import zel.protocol.typed.PacketPackage
import zel.room.listing.RoomCategory
import javax.sql.DataSource

class AchievementsModule(
    dataSource: DataSource,
    players: Players,
    schedule: (Runnable, Long) -> Unit,
    categoryOf: (Int) -> RoomCategory?,
    unixTime: () -> Int,
    accountLevels: AccountLevels,
    badgeCaches: BadgeCaches,
    chatStyles: ChatStyles,
) {
    private val messages = AchievementMessages()
    val achievements = Achievements(dataSource, players, messages, schedule, accountLevels, badgeCaches)
    val resolutions =
        Resolutions(ResolutionRows(dataSource), { rules.resolutionDays }, achievements::getAchievement)
    val packets: PacketPackage = AchievementPackets(achievements, messages)

    val tally =
        AchievementTally {
            player,
            event,
            amount,
            ->
            achievements.count(player, event, amount)
        }

    val hooks =
        AchievementHooks(
            tally = tally,
            explore = ExploreActivityAchievements(achievements, achievements::named),
            roomTime = RoomTimeAchievements(achievements, categoryOf, unixTime),
            gamePresence = GamePresenceAchievement(achievements, achievements::named),
            identity = IdentityAchievements(achievements, unixTime, chatStyles::ownedBy),
        )

    fun install() = AchievementEvents.install(hooks)
}
