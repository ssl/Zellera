package zel.protocol.incoming.moderation

import zel.core.Configuration
import zel.core.Texts
import zel.game.Game
import zel.game.moderation.PlayerClassifier
import zel.protocol.outgoing.moderation.ModeratorPlayerInfo
import zel.protocol.typed.PacketPackage
import javax.sql.DataSource

class ModerationPacketsModule(
    private val game: Game,
    private val texts: Texts,
    private val config: Configuration,
    private val dataSource: DataSource,
    private val clock: () -> Int,
) {
    fun packages(): List<PacketPackage> {
        val manager = requireNotNull(game.moderationService)
        val sanctions = requireNotNull(game.sanctions)
        val players = requireNotNull(game.players)
        val rooms = game.roomListings
        val forumThreads = requireNotNull(game.guilds).forumThreads
        val userInfo = ModeratorPlayerInfo(dataSource, sanctions, clock)
        return listOf(
            ModerationPackets(
                manager,
                sanctions,
                players,
                rooms,
                texts,
                userInfo,
                IssueChatlogs(manager, rooms, players, forumThreads),
                PlayerClassifier(dataSource, { game.rooms }, clock),
                clock,
                live = { game.rooms },
            ),
            ReportPackets(manager, players, config, forumThreads, clock),
        )
    }
}
