package zel.wired.score

import zel.room.Room
import zel.wired.box.BoxSettings
import zel.wired.form.BoxValidation
import zel.wired.form.SaveOutcome
import zel.wired.form.SaveRequest
import zel.wired.variable.Availability
import zel.wired.variable.RoomVariables
import zel.wired.variable.VariableTarget

class LeaderboardRules(
    private val variables: RoomVariables,
    private val published: (Room, Int) -> Boolean,
    private val choiceCount: (Room, Int) -> Int = { _, _ -> 0 },
) : BoxValidation {
    override fun check(
        room: Room,
        settings: BoxSettings,
    ): SaveOutcome = SaveOutcome.Saved(settings)

    override fun check(
        room: Room,
        settings: BoxSettings,
        request: SaveRequest,
    ): SaveOutcome {
        val form = LeaderboardForm(settings.copy(variableIds = request.variableIds, text = request.text))
        return when {
            !sized(
                request,
            ) || !columns(form) || !flags(form) || !overrides(form) || !texts(form) || pointer(room, form) -> {
                invalid(INVALID)
            }

            form.filterOn && !permanentUser(room, form.filterVariable) -> {
                invalid(FILTER_REQUIRED)
            }

            form.comparisonOn && form.comparisonGlobal && !global(room, form.globalVariable) -> {
                invalid(GLOBAL_REQUIRED)
            }

            choiceCount(room, request.boxId) > MAX_CHOICES -> {
                invalid(TOO_MANY_CHOICES)
            }

            published(room, request.boxId) -> {
                invalid(ROOM_LIMIT)
            }

            else -> {
                SaveOutcome.Saved(stored(settings, form))
            }
        }
    }

    private fun invalid(key: String) = SaveOutcome.Invalid("$PREFIX$key")

    private fun sized(request: SaveRequest): Boolean =
        request.ints.size in MIN_INTS..MAX_INTS && request.variableIds.size == IDS && request.text.length <= MAX_TEXT

    private fun columns(form: LeaderboardForm): Boolean {
        val count = form.columnCount
        if (count !in 1..LeaderboardForm.COLUMN_SLOTS) return false
        val used = form.columnTypes.take(count)
        val unused = form.columnTypes.drop(count)
        return used.first() == LeaderboardForm.USERNAME &&
            used.all { it in LeaderboardForm.USERNAME..LeaderboardForm.TEXT } &&
            used.distinct().size == used.size &&
            unused.all { it == UNUSED } &&
            (0 until count).all { form.label(it).length in LABEL_LENGTH }
    }

    private fun flags(form: LeaderboardForm): Boolean {
        val ints = form.ints
        val on = { at: Int -> ints.getOrElse(at) { 0 } }
        val switches =
            on(LeaderboardForm.FILTER_ON) in 0..1 &&
                on(LeaderboardForm.COMPARISON_ON) in 0..1 &&
                on(LeaderboardForm.COMPARISON_MODE) in 0..1
        val filterClear = form.filterOn || form.filterVariable == LeaderboardForm.NONE
        val comparisonClear =
            form.comparisonOn || (form.globalVariable == LeaderboardForm.NONE && form.literal.isEmpty())
        return switches && filterClear && comparisonClear && literal(form)
    }

    private fun literal(form: LeaderboardForm): Boolean {
        if (!form.comparisonOn || form.comparisonGlobal) return true
        return form.literal.toLongOrNull()?.toString() == form.literal
    }

    private fun overrides(form: LeaderboardForm): Boolean {
        val count = form.overrideCount
        if (count !in 0..LeaderboardForm.OVERRIDE_SLOTS) return false
        val used = form.overrides.take(count)
        return used.all { (id, name) -> id != LeaderboardForm.NONE && name.length in 1..MAX_OVERRIDE_NAME } &&
            used.map { it.first }.distinct().size == used.size
    }

    private fun texts(form: LeaderboardForm): Boolean =
        form.title.length in 1..MAX_TITLE &&
            form.description.length <= MAX_DESCRIPTION &&
            form.offsetX in -MAX_OFFSET..MAX_OFFSET &&
            form.pageOption in 0..MAX_PAGE_OPTION

    private fun pointer(
        room: Room,
        form: LeaderboardForm,
    ): Boolean = form.ids.any { id -> variables.find(room, id)?.entry?.availability == Availability.EXECUTION }

    private fun permanentUser(
        room: Room,
        id: String,
    ): Boolean =
        variables.find(room, id)?.entry?.let {
            it.target == VariableTarget.USER && Availability.isStored(it.availability)
        } == true

    private fun global(
        room: Room,
        id: String,
    ): Boolean = variables.find(room, id)?.entry?.target == VariableTarget.GLOBAL

    private fun stored(
        settings: BoxSettings,
        form: LeaderboardForm,
    ): BoxSettings {
        val ints = settings.ints.toMutableList()
        if (form.sort !in 0..MAX_SORT) ints[LeaderboardForm.SORT] = 0
        form.widths.forEachIndexed { i, width ->
            if (width !in 0..MAX_WIDTH) {
                ints[LeaderboardForm.WIDTHS + i] = LeaderboardForm.defaultWidth(form.columnTypes[i])
            }
        }
        return settings.copy(ints = ints)
    }

    private companion object {
        const val PREFIX = "wiredfurni.error.variable_leaderboard."
        const val INVALID = "invalid_configuration"
        const val FILTER_REQUIRED = "filter_required"
        const val GLOBAL_REQUIRED = "global_operand_required"
        const val ROOM_LIMIT = "room_limit"
        const val TOO_MANY_CHOICES = "too_many_choices"
        private const val MAX_CHOICES = 10
        const val MIN_INTS = 14
        const val MAX_INTS = 19
        const val IDS = 12
        const val MAX_TEXT = 1000
        const val UNUSED = -1
        val LABEL_LENGTH = 2..12
        const val MAX_OVERRIDE_NAME = 22
        const val MAX_TITLE = 50
        const val MAX_DESCRIPTION = 80
        const val MAX_OFFSET = 32
        const val MAX_PAGE_OPTION = 2
        const val MAX_SORT = 5
        const val MAX_WIDTH = 3
    }
}
