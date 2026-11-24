package zel.protocol.achievements

import zel.game.achievements.AchievementEntries
import zel.game.achievements.Achievements
import zel.protocol.generated.GetAchievementsInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute

class AchievementPackets(
    private val achievements: Achievements,
    messages: AchievementMessages,
) : PacketPackage {
    override val name = "achievements"

    override val routes =
        listOf(
            PacketRoute(GetAchievementsInCodec) { session, _ ->
                val player = requireNotNull(session.client.player)
                messages.list(session.client, AchievementEntries.listFor(player, achievements))
            },
        )
}
