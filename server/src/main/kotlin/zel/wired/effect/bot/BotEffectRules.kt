package zel.wired.effect.bot

import zel.wired.box.BoxSettings
import zel.wired.form.BoxValidation
import zel.wired.form.SaveOutcome

class BotEffectRules(
    private val maxName: () -> Int = { MAX_NAME },
    private val maxLine: () -> Int = { MAX_LINE },
    private val filterText: (Int, String) -> String = { _, text -> text },
) {
    private val talk =
        BoxValidation.fromRequest { s, r ->
            val mode = s.ints[BotTalk.MODE]
            if (mode != 0 && mode != BotTalk.SHOUT) {
                SaveOutcome.Invalid(INVALID_MODE)
            } else {
                SaveOutcome.Saved(clampedTalk(s, r.saverId))
            }
        }

    val validations: Map<String, BoxValidation> =
        mapOf(
            "wf_act_bot_talk" to talk,
            "wf_act_bot_talk_to_avatar" to talk,
            "wf_act_bot_follow_avatar" to
                BoxValidation { _, s ->
                    if (s.ints[BotFollowAvatar.MODE] in
                        0..1
                    ) {
                        SaveOutcome.Saved(s)
                    } else {
                        SaveOutcome.Invalid(INVALID_MODE)
                    }
                },
            "wf_act_bot_clothes" to
                // Any look saves; one the client cannot draw is never sent when the box runs.
                BoxValidation { _, s -> SaveOutcome.Saved(s.copy(text = s.text.ifEmpty { TAB })) },
        )

    private fun clampedTalk(
        settings: BoxSettings,
        saverId: Int,
    ): BoxSettings {
        val ints = settings.ints
        val name = BotChoice.name(settings.text).take(maxName())
        val line = filterText(saverId, BotChoice.rest(settings.text)).take(maxLine())
        val text = if (settings.text.contains(TAB) || settings.text.isEmpty()) name + TAB + line else name
        return settings.copy(
            ints =
                listOf(
                    ints[BotTalk.MODE],
                    ints[BotTalk.WIDTH].coerceIn(-1, MAX_WIDTH),
                    if (ints[BotTalk.HIDE_NAME] != 0) 1 else 0,
                ),
            text = text,
        )
    }

    companion object {
        const val INVALID_MODE = "Choose one of the modes."
        const val MAX_NAME = 100
        const val MAX_LINE = 100
        private const val MAX_WIDTH = 2
        private const val TAB = "\t"
    }
}
