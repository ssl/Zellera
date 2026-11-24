package zel.wired.firing

import zel.room.event.RoomEvent

class FiringStart(
    val unit: Int?,
    val furni: Int?,
    val reachedUnit: Int? = null,
    val clicked: Int? = null,
    val selection: Selection = Selection(),
    val signal: SignalPayload? = null,
    val context: MutableMap<String, HeldValue>? = null,
) {
    companion object {
        fun of(event: RoomEvent): FiringStart =
            when (event) {
                is RoomEvent.Says -> FiringStart(event.unitIndex, null)
                is RoomEvent.Entered -> FiringStart(event.unitIndex, null)
                is RoomEvent.Leaving -> FiringStart(event.unitIndex, null)
                is RoomEvent.Left -> FiringStart(event.unitIndex, null)
                is RoomEvent.StepOn -> FiringStart(event.unitIndex, event.furniId)
                is RoomEvent.StepOff -> FiringStart(event.unitIndex, event.furniId)
                is RoomEvent.ClickFurni -> FiringStart(event.unitIndex, event.furniId)
                is RoomEvent.ClickTile -> FiringStart(event.unitIndex, event.furniId)
                is RoomEvent.ClickUser -> FiringStart(event.unitIndex, null, clicked = event.clickedIndex)
                is RoomEvent.StateChanged -> FiringStart(event.unitIndex, event.furniId)
                is RoomEvent.Action -> FiringStart(event.unitIndex, null)
                is RoomEvent.BotReachedFurni -> FiringStart(event.botIndex, event.furniId)
                is RoomEvent.BotReachedUnit -> FiringStart(event.botIndex, null, reachedUnit = event.reachedIndex)
                is RoomEvent.Collision -> FiringStart(event.unitIndex, event.furniId)
                is RoomEvent.ScoreChanged -> FiringStart(event.unitIndex, null)
                RoomEvent.GameStarted, RoomEvent.GameEnded, RoomEvent.Initialized -> FiringStart(null, null)
            }
    }
}
