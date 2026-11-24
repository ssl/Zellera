package zel.room.games

import zel.core.Configuration

data class TileClaimRules(
    val hijackPoints: Int = 0,
    val fillPoints: Int = 0,
    val lockPoints: Int = 1,
) {
    companion object {
        fun read(config: Configuration): TileClaimRules =
            TileClaimRules(
                hijackPoints = config.getInt("tileclaim.points.per_taken_tile", 0),
                fillPoints = config.getInt("tileclaim.points.per_filled_tile", 0),
                lockPoints = config.getInt("tileclaim.points.per_locked_tile", 1),
            )
    }
}
