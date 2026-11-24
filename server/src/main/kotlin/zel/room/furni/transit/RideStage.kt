package zel.room.furni.transit

sealed interface RideStage {
    data object Entering : RideStage

    data object Inside : RideStage

    data object Closing : RideStage

    data object Jump : RideStage

    data object Exit : RideStage

    data object Leaving : RideStage
}
