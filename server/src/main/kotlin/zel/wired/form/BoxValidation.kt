package zel.wired.form

import zel.room.Room
import zel.wired.box.BoxSettings

fun interface BoxValidation {
    fun check(
        room: Room,
        settings: BoxSettings,
    ): SaveOutcome

    /** Sees the message as sent, before the fresh ints fill it in. */
    fun check(
        room: Room,
        settings: BoxSettings,
        request: SaveRequest,
    ): SaveOutcome = check(room, settings)

    companion object {
        val None: BoxValidation = BoxValidation { _, settings -> SaveOutcome.Saved(settings) }

        fun fromRequest(rule: (BoxSettings, SaveRequest) -> SaveOutcome): BoxValidation =
            object : BoxValidation {
                override fun check(
                    room: Room,
                    settings: BoxSettings,
                ): SaveOutcome = SaveOutcome.Saved(settings)

                override fun check(
                    room: Room,
                    settings: BoxSettings,
                    request: SaveRequest,
                ): SaveOutcome = rule(settings, request)
            }
    }
}
