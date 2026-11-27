package zel.room.rights

import zel.game.players.Player
import zel.room.RoomScope
import zel.room.unit.RoomUnit

interface ModerationHost {
    fun player(id: Int): Player?

    /** Runs [job] off the room lane; jobs with an equal [key] run in the order given. */
    fun persistInOrder(
        key: Any,
        job: Runnable,
    )

    fun nowSeconds(): Int

    fun reaches(
        staff: Player,
        targetId: Int,
    ): Boolean = false

    fun walkOut(
        scope: RoomScope,
        unit: RoomUnit,
    )

    fun progress(
        playerId: Int,
        achievement: String,
        amount: Int,
    ) {}
}
