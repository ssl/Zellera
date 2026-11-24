package zel.protocol.camera

import zel.core.Configuration
import zel.game.players.PlayerWallet

internal data class CameraPrices(
    val photo: Price,
    val publish: Price,
) {
    data class Price(
        val credits: Int,
        val points: Int,
        val pointsType: Int,
    ) {
        /** Takes the whole price or nothing: points that cannot be taken give the credits back. */
        fun takeFrom(
            wallet: PlayerWallet,
            operationId: String,
        ): Boolean {
            if (credits > 0 && !wallet.takeCredits(credits)) return false
            if (points > 0 && !wallet.takePoints(pointsType, points, REASON, operationId, null)) {
                if (credits > 0) wallet.changeCredits(credits)
                return false
            }
            return true
        }

        fun refundTo(wallet: PlayerWallet) {
            if (credits > 0) wallet.changeCredits(credits)
            if (points > 0) wallet.changePoints(pointsType, points)
        }
    }

    companion object {
        private const val REASON = "camera.pay"
        private const val PHOTO_CREDITS = 2
        private const val PUBLISH_POINTS = 10

        fun from(config: Configuration) =
            CameraPrices(
                photo =
                    Price(
                        config.getInt("camera.photo.price.credits", PHOTO_CREDITS),
                        config.getInt("camera.photo.price.points", 0),
                        config.getInt("camera.photo.price.points_type", 0),
                    ),
                publish =
                    Price(
                        0,
                        config.getInt("camera.publish.price.points", PUBLISH_POINTS),
                        config.getInt("camera.publish.price.points_type", 0),
                    ),
            )
    }
}
