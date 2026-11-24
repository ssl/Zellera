package zel.wired.trigger

import zel.content.ClientStrings
import zel.protocol.generated.NotificationDialogOut
import zel.room.Room
import zel.room.furni.place.Interactions
import zel.wired.BoxSettingsSource

/**
 * The wired notice an entrant gets when the room has a says trigger in contains mode whose text holds a `#(` or
 * `$(` marker: one notice per room load, right after RoomEntryInfo.
 */
object TextCaptureNotice {
    private const val CODE = "wf_trg_says_something"
    private val MARKERS = listOf("#(", "$(")

    fun applies(
        room: Room,
        forms: BoxSettingsSource,
    ): Boolean =
        room.furni.all().any { furni ->
            Interactions.of(furni.definition) == CODE &&
                forms.settings(room, furni)?.let { settings ->
                    SaysSomething.inContainsMode(settings) && MARKERS.any { it in settings.text }
                } == true
        }

    fun dialog(): NotificationDialogOut =
        NotificationDialogOut(
            "wired.error",
            listOf(
                NotificationDialogOut.List1Item("image", ClientStrings.badgeImage("WIRD1")),
                NotificationDialogOut.List1Item("message", "\${room.error.wired.trigger_text_capture}"),
            ),
        )
}
