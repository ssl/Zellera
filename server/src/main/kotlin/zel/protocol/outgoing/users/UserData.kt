package zel.protocol.outgoing.users

import zel.core.rules.rules
import zel.game.players.Player
import zel.protocol.generated.PlayerObjectOut
import zel.protocol.generated.PlayerObjectOutCodec
import zel.protocol.outgoing.OutgoingMessage
import zel.util.GameZone
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

class UserData(
    val player: Player,
) : OutgoingMessage<PlayerObjectOut>(PlayerObjectOutCodec) {
    override fun message(): PlayerObjectOut {
        val profile = player.profile
        val stats = player.stats
        return PlayerObjectOut(
            int1 = profile.id,
            string1 = profile.name,
            string2 = profile.figure,
            string3 = profile.gender.name.uppercase(),
            string4 = profile.motto,
            string5 = "",
            boolean1 = false,
            int2 = stats.tally.respectsReceived,
            int3 = stats.tally.respectsLeft,
            int4 = stats.tally.petRespectsLeft,
            boolean2 = true,
            string6 = LAST_ACCESS_FORMAT.format(Instant.ofEpochSecond(profile.lastOnlineAt.toLong())),
            boolean3 = stats.standing.mayChangeName,
            boolean4 = stats.standing.safetyLocked,
            trailing1 = PlayerObjectOut.Trailing1(!stats.standing.mayTrade(), ""),
            trailing2 =
                PlayerObjectOut.Trailing2(
                    stats.tally.respectRefillsLeft,
                    rules.dailyRespects,
                ),
        )
    }

    private companion object {
        val LAST_ACCESS_FORMAT: DateTimeFormatter
            get() = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss", Locale.ROOT).withZone(GameZone.current)
    }
}
