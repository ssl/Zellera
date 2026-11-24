package zel.wired.form

import zel.wired.box.BoxSettings

sealed interface SaveOutcome {
    data class Saved(
        val settings: BoxSettings,
    ) : SaveOutcome

    data class Invalid(
        val key: String,
        val params: List<Pair<String, String>> = emptyList(),
        val whisper: String? = null,
    ) : SaveOutcome

    /** Refused with only an ErrorReport carrying the save's message id. */
    data object Report : SaveOutcome

    /** Dropped without any answer. */
    data object Ignored : SaveOutcome
}
