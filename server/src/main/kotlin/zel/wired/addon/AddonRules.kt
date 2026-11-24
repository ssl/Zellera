package zel.wired.addon

import zel.room.Room
import zel.room.furni.place.Interactions
import zel.wired.box.BoxSettings
import zel.wired.form.BoxValidation
import zel.wired.form.SaveOutcome
import zel.wired.form.SaveRequest
import zel.wired.signal.LinkMessages

class AddonRules(
    private val superWired: (Int) -> Boolean = { false },
) {
    val validations: Map<String, BoxValidation> =
        mapOf(
            "wf_xtra_text_output_username" to BoxValidation { _: Room, settings -> placeholder(settings) },
            "wf_xtra_text_output_furni_name" to BoxValidation { _: Room, settings -> placeholder(settings) },
            "wf_xtra_mov_curve" to BoxValidation { _: Room, settings -> jumpStrength(settings) },
            "wf_xtra_filter_furni" to BoxValidation { _: Room, settings -> filterAmount(settings) },
            "wf_xtra_filter_users" to BoxValidation { _: Room, settings -> filterAmount(settings) },
            "wf_xtra_execution_limit" to BoxValidation { _: Room, settings -> executionLimit(settings) },
            LinkSpeaker.CODE to BoxValidation { room, settings -> linkSpeaker(room, settings) },
            AchievementEnabler.CODE to BoxValidation.fromRequest(::enabler),
        )

    /** Both numbers are held to 1..100. */
    private fun executionLimit(settings: BoxSettings): SaveOutcome =
        SaveOutcome.Saved(settings.copy(ints = List(2) { settings.ints.getOrElse(it) { 1 }.coerceIn(1, LIMIT_MAX) }))

    private fun enabler(
        settings: BoxSettings,
        request: SaveRequest,
    ): SaveOutcome =
        if (superWired(request.saverId)) {
            SaveOutcome.Saved(settings.copy(text = AchievementEnabler.kept(settings.text)))
        } else {
            SaveOutcome.Invalid(SAVE_FAILED)
        }

    private fun placeholder(settings: BoxSettings): SaveOutcome {
        val name = PlaceholderText(settings).name
        return when {
            name.isEmpty() || name.length > PlaceholderText.MAX_NAME -> SaveOutcome.Invalid(NAME_LENGTH)
            !SYNTAX.matches(name) -> SaveOutcome.Invalid(NAME_SYNTAX)
            name.equals(RESERVED_NAME, ignoreCase = true) -> SaveOutcome.Invalid(RESERVED)
            else -> SaveOutcome.Saved(settings.copy(text = lowerName(settings.text)))
        }
    }

    private fun lowerName(text: String): String {
        val parts = text.split('\t')
        return (listOf(parts.first().lowercase()) + parts.drop(1)).joinToString("\t")
    }

    private fun jumpStrength(settings: BoxSettings): SaveOutcome {
        val ints =
            settings.ints.mapIndexed { index, value ->
                if (index == STRENGTH) value.coerceIn(-MAX_STRENGTH, MAX_STRENGTH) else value
            }
        return SaveOutcome.Saved(settings.copy(ints = ints))
    }

    /** A typed amount under 1 is saved as 1. */
    private fun filterAmount(settings: BoxSettings): SaveOutcome {
        val ints =
            settings.ints.mapIndexed { index, value ->
                if (index == FILTER_AMOUNT) value.coerceAtLeast(MIN_FILTER_AMOUNT) else value
            }
        return SaveOutcome.Saved(settings.copy(ints = ints))
    }

    private fun linkSpeaker(
        room: Room,
        settings: BoxSettings,
    ): SaveOutcome {
        val onlyLinks =
            settings.picks.all { id ->
                room.furni[id]?.let { Interactions.of(it.definition) } ==
                    LinkMessages.LINK
            }
        if (!onlyLinks) return SaveOutcome.Invalid(REQUIRE_LINK)
        val ticks = listOf(0, 1).map { if (settings.intAt(it, 1) != 0) 1 else 0 }
        return SaveOutcome.Saved(settings.copy(ints = ticks, text = ""))
    }

    companion object {
        const val NAME_LENGTH = "wiredfurni.error.placeholder.name_length"
        const val NAME_SYNTAX = "wiredfurni.error.placeholder.name_syntax"
        const val RESERVED = "wiredfurni.error.placeholder.reserved"
        const val REQUIRE_LINK = "wiredfurni.error.require_link_furni"
        const val SAVE_FAILED = "This effect cannot be saved with these settings."
        private const val RESERVED_NAME = "trigger_text"
        private const val STRENGTH = 1
        private const val MAX_STRENGTH = 1000
        private const val FILTER_AMOUNT = 0
        private const val MIN_FILTER_AMOUNT = 1
        private const val LIMIT_MAX = 100
        private val SYNTAX = Regex("[A-Za-z0-9_]+")
    }
}
