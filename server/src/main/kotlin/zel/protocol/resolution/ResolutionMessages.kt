package zel.protocol.resolution

import zel.game.achievements.resolution.ResolutionChoice
import zel.net.clients.ClientConnection
import zel.protocol.generated.AchievementResolutionCompletedOut
import zel.protocol.generated.AchievementResolutionCompletedOutCodec
import zel.protocol.generated.AchievementResolutionProgressOut
import zel.protocol.generated.AchievementResolutionProgressOutCodec
import zel.protocol.generated.AchievementResolutionsOut
import zel.protocol.generated.AchievementResolutionsOutCodec

class ResolutionMessages {
    fun picker(
        client: ClientConnection,
        stuffId: Int,
        choices: List<ResolutionChoice>,
        secondsLeft: Int,
    ) = client.send(
        AchievementResolutionsOutCodec,
        AchievementResolutionsOut(
            stuffId,
            choices.map {
                AchievementResolutionsOut.List1Item(
                    it.achievementId,
                    it.level,
                    it.badgeCode,
                    it.requiredLevel,
                    it.state.code,
                )
            },
            secondsLeft,
        ),
    )

    fun completed(
        client: ClientConnection,
        stuffCode: String,
        badge: String,
    ) = client.send(
        AchievementResolutionCompletedOutCodec,
        AchievementResolutionCompletedOut(stuffCode, badge),
    )

    fun progress(
        client: ClientConnection,
        stuffId: Int,
        achievementId: Int,
        achievementName: String,
        currentProgress: Int,
        progressNeeded: Int,
        timeLeft: Int,
    ) = client.send(
        AchievementResolutionProgressOutCodec,
        AchievementResolutionProgressOut(
            stuffId,
            achievementId,
            achievementName,
            currentProgress,
            progressNeeded,
            timeLeft,
        ),
    )
}
