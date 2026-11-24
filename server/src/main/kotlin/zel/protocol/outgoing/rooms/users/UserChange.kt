package zel.protocol.outgoing.rooms.users

import zel.game.players.Player
import zel.protocol.generated.RoomUnitChangeOut
import zel.protocol.generated.RoomUnitChangeOutCodec
import zel.protocol.outgoing.OutgoingMessage

class UserChange(
    private val player: Player,
    private val roomIndex: Int = OWN_CHANGE,
) : OutgoingMessage<RoomUnitChangeOut>(RoomUnitChangeOutCodec) {
    override fun message() =
        RoomUnitChangeOut(
            roomIndex,
            player.profile.figure,
            player.profile.gender.name,
            player.profile.motto,
            player.stats.tally.achievementScore,
            "",
            emptyList(),
            player.services.badgeCaches.leaderboard
                .totalBadgesRank(player.profile.id),
        )

    private companion object {
        const val OWN_CHANGE = -1
    }
}
