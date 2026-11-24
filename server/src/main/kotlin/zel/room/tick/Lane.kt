package zel.room.tick

sealed interface Lane {
    data object Now : Lane

    data object AfterUnitUpdate : Lane

    data object EndOfTick : Lane

    data object NextTick : Lane

    data object NextTickAfterUnitUpdate : Lane
}
