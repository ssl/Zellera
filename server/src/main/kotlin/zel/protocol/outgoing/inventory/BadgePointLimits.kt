package zel.protocol.outgoing.inventory

import zel.game.achievements.Achievements
import zel.protocol.generated.BadgePointLimitsOut
import zel.protocol.generated.BadgePointLimitsOutCodec
import zel.protocol.outgoing.OutgoingMessage

class BadgePointLimits(
    private val manager: Achievements,
) : OutgoingMessage<BadgePointLimitsOut>(BadgePointLimitsOutCodec) {
    override fun message(): BadgePointLimitsOut = of(manager)

    companion object {
        fun of(manager: Achievements): BadgePointLimitsOut =
            BadgePointLimitsOut(
                manager.all.values.map { achievement ->
                    BadgePointLimitsOut.BadgesItem(
                        badgeCode = achievement.name.removePrefix("ACH_"),
                        levels =
                            achievement.levels.values.map {
                                BadgePointLimitsOut.BadgesItem.LevelsItem(it.level, it.progressNeeded)
                            },
                    )
                },
            )
    }
}
