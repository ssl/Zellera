package zel.room.games

import zel.game.quests.CountedAction
import zel.room.RoomScope
import zel.room.furni.RoomFurni

interface GameHost {
    fun nowSeconds(): Int = 0

    fun figure(playerId: Int): PlayerFigure? = null

    fun wear(
        scope: RoomScope,
        playerId: Int,
        look: String,
    ) {}

    fun outfitOver(
        look: String,
        outfit: String,
        gender: String,
        playerId: Int,
    ): String = outfit

    fun record(game: GameRecord) {}

    fun finished(
        scope: RoomScope,
        result: GameResult,
    ) {}

    fun clockCounted(
        scope: RoomScope,
        clock: RoomFurni,
        millis: Int,
    ) {}

    /** A player's game action the quests and tasks count, off the room thread. */
    fun counted(
        playerId: Int,
        goal: CountedAction,
        amount: Int,
    ) {}

    /** One game action whose [size] a quest measures, off the room thread. */
    fun reached(
        playerId: Int,
        goal: CountedAction,
        size: Int,
    ) {}

    companion object {
        val None: GameHost = object : GameHost {}
    }
}
