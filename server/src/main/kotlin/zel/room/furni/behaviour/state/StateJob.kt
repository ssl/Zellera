package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.furni.RoomFurni

/** [settled] is stored if it never runs, so no passing state is saved. */
class StateJob(
    val due: Long,
    val furniId: Int,
    val settled: String?,
    val run: (RoomScope, RoomFurni) -> Unit,
    /** Runs instead of [run] when the job is dropped, to undo what must not outlive the furni. */
    val dropped: (RoomScope) -> Unit = {},
)
