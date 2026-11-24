package zel.room.games

import zel.core.Configuration

data class FreezeRules(
    val powerUpPoints: Int = 0,
    val powerUpChance: Int = 0,
    val protectSeconds: Int = 0,
    val blockPoints: Int = 0,
    val frozenSeconds: Int = 0,
    val frozenLoseSnowballs: Int = 0,
    val frozenLoseBoost: Int = 0,
    val maxLives: Int = 0,
    val maxSnowballs: Int = 0,
    val frozenLosePoints: Int = DEFAULT_FROZEN_LOSE_POINTS,
    val protectionStacks: Boolean = false,
) {
    companion object {
        const val DEFAULT_FROZEN_LOSE_POINTS = 10

        fun read(config: Configuration): FreezeRules =
            FreezeRules(
                powerUpPoints = config.getInt("freeze.points.per_power_up"),
                powerUpChance = config.getInt("freeze.power_up.chance.percent"),
                protectSeconds = config.getInt("freeze.shield.duration.seconds"),
                blockPoints = config.getInt("freeze.points.per_broken_block"),
                frozenSeconds = config.getInt("freeze.frozen.duration.seconds"),
                frozenLoseSnowballs = config.getInt("freeze.frozen.snowballs_lost"),
                frozenLoseBoost = config.getInt("freeze.frozen.boost_lost"),
                maxLives = config.getInt("freeze.power_up.max_lives"),
                maxSnowballs = config.getInt("freeze.power_up.max_snowballs"),
                frozenLosePoints = config.getInt("freeze.frozen.points_lost", DEFAULT_FROZEN_LOSE_POINTS),
                protectionStacks = config.getBoolean("freeze.shield.time_adds_up"),
            )
    }
}
