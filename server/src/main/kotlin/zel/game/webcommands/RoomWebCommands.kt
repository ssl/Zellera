package zel.game.webcommands

import zel.util.MILLIS_PER_SECOND

/** `askRoomQuestion`: staff put a like-or-dislike question to an open room for a number of seconds. */
internal class RoomWebCommands(
    private val services: WebCommandApplier.Services,
) {
    fun askQuestion(payload: WebCommandPayload): WebCommandResult {
        val roomId =
            payload.int("roomId")?.takeIf { it > 0 } ?: return WebCommandResult.failed("The payload needs roomId.")
        val question = payload.string("question")?.trim().orEmpty()
        if (question.isEmpty() || question.length > QUESTION_MAX) {
            return WebCommandResult.failed("A question is 1 to $QUESTION_MAX characters.")
        }
        val seconds = payload.int("seconds") ?: DEFAULT_SECONDS
        if (seconds !in SECONDS) return WebCommandResult.failed("Seconds are ${SECONDS.first} to ${SECONDS.last}.")
        val asked = services.rooms().askRoomQuestion(roomId, question, seconds * MILLIS_PER_SECOND)
        return if (asked) {
            WebCommandResult.done("Asked in room $roomId for $seconds seconds.")
        } else {
            WebCommandResult.failed("Room $roomId is not open, or it is asking a question already.")
        }
    }

    companion object {
        const val QUESTION_MAX = 255
        const val DEFAULT_SECONDS = 60
        val SECONDS = 5..3600
    }
}
