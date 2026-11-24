package zel.wired.addon

import zel.protocol.generated.WiredEnvironmentOut
import zel.protocol.generated.WiredEnvironmentOutCodec
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.BoxSettingsSource
import zel.wired.trigger.ClickUser

class AchievementEnvironment(
    private val forms: BoxSettingsSource,
) {
    fun message(scope: RoomScope): WiredEnvironmentOut =
        WiredEnvironmentOut(
            scope.room.hearsUserClicks ||
                scope.room.furni
                    .withInteraction(ClickUser.CODE)
                    .isNotEmpty(),
            WiredEnvironmentOut.Trailing1(AchievementEnabler.namesIn(scope.room, forms)),
        )

    fun resend(scope: RoomScope) {
        scope.out.send(Lane.Now, Audience.Room, WiredEnvironmentOutCodec, message(scope))
    }
}
