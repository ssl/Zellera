package zel.wired.form

import zel.room.Room
import zel.wired.box.BoxForm
import zel.wired.box.BoxSettings
import zel.wired.box.SourceSlot
import zel.wired.firing.FurniSource
import zel.wired.firing.UserSource
import kotlin.math.abs

object SaveChecks {
    const val INVALID_USER_SOURCE_COUNT = "wiredfurni.error.invalid_user_source_count"
    const val INVALID_USER_SOURCE = "wiredfurni.error.invalid_user_source"
    const val INVALID_FURNI_SOURCE_COUNT = "wiredfurni.error.invalid_furni_source_count"
    const val INVALID_FURNI_SOURCE = "wiredfurni.error.invalid_furni_source"

    const val TOO_MUCH_TEXT = "wiredfurni.error.too_much_text"
    private const val LIMIT = "limit"

    /** No form takes a longer text; a longer one is not looked at. */
    const val WIRE_TEXT_LIMIT = 8192

    /** Longer than any id the server hands out: an internal name, or a shared variable's room and box. */
    const val VARIABLE_ID_LIMIT = 64

    private val KNOWN_FURNI =
        setOf(
            FurniSource.TRIGGERING,
            FurniSource.PICKED,
            FurniSource.PICKED_SECOND,
            FurniSource.SNAPSHOT,
            FurniSource.SELECTION,
            FurniSource.SIGNAL,
            FurniSource.ALL,
            FurniSource.MOVED,
        )
    private val KNOWN_USER =
        setOf(
            UserSource.TRIGGERING,
            UserSource.REACHED,
            UserSource.CLICKED,
            UserSource.BY_NAME,
            UserSource.BY_NAME_SECOND,
            UserSource.SELECTION,
            UserSource.SIGNAL,
            UserSource.ALL,
        )

    fun check(
        room: Room,
        spec: DialogSpec,
        request: SaveRequest,
        validation: BoxValidation,
        selectionLimit: Int = BoxForm.FURNI_LIMIT,
    ): SaveOutcome {
        val form = spec.form
        val pickLimit = form.furniLimit ?: selectionLimit
        return when {
            request.picks.size > pickLimit || request.picks2.size > pickLimit -> {
                SaveOutcome.Report
            }

            request.variableIds.size > form.fresh.variableIds.size ||
                request.variableIds.any { it.length > VARIABLE_ID_LIMIT } -> {
                SaveOutcome.Report
            }

            request.text.length > WIRE_TEXT_LIMIT -> {
                tooMuchText(form)
            }

            request.userSources.size > form.userSlots.size -> {
                SaveOutcome.Invalid(INVALID_USER_SOURCE_COUNT)
            }

            request.furniSources.size > form.furniSlots.size -> {
                SaveOutcome.Invalid(INVALID_FURNI_SOURCE_COUNT)
            }

            request.furniSources.any { it !in KNOWN_FURNI } || request.userSources.any { it !in KNOWN_USER } -> {
                SaveOutcome.Report
            }

            !offered(request.userSources, SourceOffers.userSlots(room, form.userSlots)) -> {
                SaveOutcome.Invalid(INVALID_USER_SOURCE)
            }

            !offered(request.furniSources, form.furniSlots) -> {
                SaveOutcome.Invalid(INVALID_FURNI_SOURCE)
            }

            else -> {
                fits(form, validation.check(room, settings(spec, request), request))
            }
        }
    }

    /** What a box's rule left of the text has to fit the form. */
    private fun fits(
        form: BoxForm,
        outcome: SaveOutcome,
    ): SaveOutcome =
        if (outcome is SaveOutcome.Saved && outcome.settings.text.length > form.textLimit) {
            tooMuchText(form)
        } else {
            outcome
        }

    private fun tooMuchText(form: BoxForm): SaveOutcome =
        SaveOutcome.Invalid(TOO_MUCH_TEXT, listOf(LIMIT to form.textLimit.toString()))

    /** A stored row as a save would have left it: what is past the form's bounds is cut, not refused. */
    fun bounded(
        form: BoxForm,
        settings: BoxSettings,
    ): BoxSettings {
        val ids = settings.variableIds
        val fits =
            settings.text.length <= form.textLimit &&
                ids.size <= form.fresh.variableIds.size &&
                ids.none { it.length > VARIABLE_ID_LIMIT }
        if (fits) return settings
        return settings.copy(
            text = settings.text.take(form.textLimit),
            variableIds = ids.take(form.fresh.variableIds.size).map { it.take(VARIABLE_ID_LIMIT) },
        )
    }

    fun settings(
        spec: DialogSpec,
        request: SaveRequest,
    ): BoxSettings {
        val fresh = spec.form.fresh
        return BoxSettings(
            ints = fresh.ints.indices.map { i -> request.ints.getOrElse(i) { fresh.ints[i] } },
            text = request.text,
            picks = highestFirst(request.picks),
            picks2 = highestFirst(request.picks2),
            furniSources = filled(request.furniSources, spec.form.furniSlots),
            userSources = filled(request.userSources, spec.form.userSlots),
            quantifier = request.quantifier,
            delay = request.delay,
            variableIds = request.variableIds,
            filter = request.filter,
            invert = request.invert,
        )
    }

    /** A save stores the picks highest id first; a wall item counts by its id, sent negative. */
    private fun highestFirst(picks: List<Int>): List<Int> = picks.sortedByDescending { abs(it) }

    private fun offered(
        sources: List<Int>,
        slots: List<SourceSlot>,
    ): Boolean = sources.withIndex().all { (i, source) -> source in slots[i].offered }

    private fun filled(
        sources: List<Int>,
        slots: List<SourceSlot>,
    ): List<Int> = slots.mapIndexed { i, slot -> sources.getOrElse(i) { slot.fresh } }
}
