package zel.room.event

sealed interface RoomEvent {
    data class Says(
        val unitIndex: Int,
        val text: String,
        val shout: Boolean,
        val styleId: Int,
        val hiding: LineHiding = LineHiding(),
        /** The line as said, its trailing spaces kept. */
        val said: String = text,
    ) : RoomEvent

    data class Entered(
        val unitIndex: Int,
    ) : RoomEvent

    /**
     * The player's unit is on its way out but still in the room, with its variables, for the leave trigger: its
     * firing knows who left and can read their variables. What that firing sends does not reach the player who leaves.
     */
    data class Leaving(
        val unitIndex: Int,
    ) : RoomEvent

    /** The unit is gone from the room; parts drop what they held for it. */
    data class Left(
        val unitIndex: Int,
    ) : RoomEvent

    data class StepOn(
        val unitIndex: Int,
        val furniId: Int,
    ) : RoomEvent

    data class StepOff(
        val unitIndex: Int,
        val furniId: Int,
    ) : RoomEvent

    data class ClickFurni(
        val unitIndex: Int,
        val furniId: Int,
    ) : RoomEvent

    data class ClickTile(
        val unitIndex: Int,
        val furniId: Int,
    ) : RoomEvent

    data class ClickUser(
        val unitIndex: Int,
        val clickedIndex: Int,
        val menu: ClickMenu = ClickMenu(),
    ) : RoomEvent

    /** [settled]: a furni's own timed result (a wheel of fortune landing, a dice's face); fires wf_trg_stuff_state. */
    data class StateChanged(
        val furniId: Int,
        val unitIndex: Int?,
        val settled: Boolean = false,
    ) : RoomEvent

    data class Action(
        val unitIndex: Int,
        val action: Int,
        val param: String,
    ) : RoomEvent

    data class BotReachedFurni(
        val botIndex: Int,
        val furniId: Int,
    ) : RoomEvent

    data class BotReachedUnit(
        val botIndex: Int,
        val reachedIndex: Int,
    ) : RoomEvent

    data class Collision(
        val unitIndex: Int,
        val furniId: Int,
    ) : RoomEvent

    data object GameStarted : RoomEvent

    data object GameEnded : RoomEvent

    data class ScoreChanged(
        val unitIndex: Int,
        val team: Int,
        val oldTotal: Int,
        val newTotal: Int,
    ) : RoomEvent

    data object Initialized : RoomEvent
}
