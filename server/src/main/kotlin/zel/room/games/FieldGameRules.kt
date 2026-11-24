package zel.room.games

import zel.core.Configuration

/** The settings of the freeze and tileclaim field games. */
interface FieldGameRules {
    val freeze: FreezeRules
    val tileclaim: TileClaimRules

    companion object {
        fun of(config: Configuration): FieldGameRules =
            object : FieldGameRules {
                override val freeze get() = FreezeRules.read(config)
                override val tileclaim get() = TileClaimRules.read(config)
            }
    }
}
