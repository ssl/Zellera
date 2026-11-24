package zel.wired.variable.box

import zel.room.Room
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.wired.box.BoxSettings
import zel.wired.form.BoxValidation
import zel.wired.form.SaveOutcome
import zel.wired.form.SaveRequest
import zel.wired.usage.UsageAllowance
import zel.wired.usage.UsageLimit
import zel.wired.variable.Availability
import zel.wired.variable.RoomVariables
import zel.wired.variable.SharedToken
import zel.wired.variable.VariableTarget

class VariableRules(
    private val variables: RoomVariables,
    private val allowance: (Room) -> UsageAllowance = { UsageLimit.of(0) },
) {
    val validations: Map<String, BoxValidation> =
        mapOf(
            RoomVariables.USER_BOX to
                definition(userAvailability, limited(VariableTarget.USER, 0, UsageAllowance::userVariables)),
            RoomVariables.FURNI_BOX to
                definition(furniAvailability, limited(VariableTarget.FURNI, 1, UsageAllowance::furniVariables)),
            RoomVariables.ROOM_BOX to
                definition(roomAvailability, limited(VariableTarget.GLOBAL, 0, UsageAllowance::roomVariables)),
            RoomVariables.CONTEXT_BOX to definition({ _, _ -> null }),
            RoomVariables.POINTER_BOX to definition(::pointerAccessor),
            RoomVariables.REFERENCE_BOX to Reference(),
            RoomVariables.ECHO_BOX to Echo(),
            "wf_trg_var_changed" to Changed(),
            "wf_act_give_var" to namesVariable(PARAMS_MISSING),
            "wf_act_change_var_val" to namesVariable(PARAMS_MISSING),
            "wf_act_remove_var" to namesVariable(PARAMS_MISSING),
            "wf_cnd_has_var" to namesVariable(CONDITION_ERROR),
            "wf_cnd_neg_has_var" to namesVariable(CONDITION_ERROR),
            "wf_cnd_var_val_match" to namesVariable(CONDITION_ERROR),
            "wf_cnd_var_age_match" to namesVariable(CONDITION_ERROR),
            "wf_slc_furni_with_var" to namesVariable(SELECTOR_ERROR),
            "wf_slc_users_with_var" to namesVariable(SELECTOR_ERROR),
        )

    private fun limited(
        target: Int,
        availabilityInt: Int,
        allowed: (UsageAllowance) -> Int,
    ): (Room, Int, BoxSettings) -> SaveOutcome.Invalid? =
        { room, boxId, settings ->
            val stored = Availability.isStored(settings.ints.getOrElse(availabilityInt) { Availability.WHILE_IN_ROOM })
            val others =
                variables.customs(room).count {
                    it.boxId != boxId && it.entry.target == target && Availability.isStored(it.entry.availability)
                }
            if (stored && others >= allowed(allowance(room))) SaveOutcome.Invalid(PERMANENT_LIMIT) else null
        }

    private fun definition(
        extra: (Room, BoxSettings) -> SaveOutcome.Invalid?,
        limit: (Room, Int, BoxSettings) -> SaveOutcome.Invalid? = { _, _, _ -> null },
    ): BoxValidation =
        object : BoxValidation {
            override fun check(
                room: Room,
                settings: BoxSettings,
            ): SaveOutcome = SaveOutcome.Saved(settings)

            override fun check(
                room: Room,
                settings: BoxSettings,
                request: SaveRequest,
            ): SaveOutcome {
                val name = cleaned(settings.text)
                val cleaned = settings.copy(text = name)
                val problem =
                    nameProblem(room, name, request.boxId)
                        ?: extra(room, cleaned)
                        ?: limit(room, request.boxId, cleaned)
                return problem ?: saved(room, request.boxId, cleaned)
            }
        }

    private fun saved(
        room: Room,
        boxId: Int,
        settings: BoxSettings,
    ): SaveOutcome {
        room.furni[boxId]?.let { variables.redefined(room, it, settings) }
        return SaveOutcome.Saved(settings)
    }

    private fun cleaned(name: String): String = name.replace(WHITESPACE, "_").lowercase()

    private fun nameProblem(
        room: Room,
        name: String,
        boxId: Int,
    ): SaveOutcome.Invalid? =
        when {
            name.isEmpty() || name.length > MAX_NAME -> {
                SaveOutcome.Invalid(NAME_LENGTH)
            }

            !name.matches(NAME_SYNTAX) -> {
                SaveOutcome.Invalid(NAME_SYNTAX_ERROR)
            }

            variables.customs(room).any { it.boxId != boxId && it.entry.name.equals(name, ignoreCase = true) } -> {
                SaveOutcome.Invalid(NAME_UNIQ)
            }

            else -> {
                null
            }
        }

    private fun pointerAccessor(
        room: Room,
        settings: BoxSettings,
    ): SaveOutcome.Invalid? {
        val accessor = variables.find(room, settings.variableIds.firstOrNull().orEmpty())
        return if (accessor?.entry?.target == VariableTarget.CONTEXT) null else SaveOutcome.Invalid(MISSING_VARIABLE)
    }

    private inner class Reference : BoxValidation {
        override fun check(
            room: Room,
            settings: BoxSettings,
        ): SaveOutcome = SaveOutcome.Saved(settings)

        override fun check(
            room: Room,
            settings: BoxSettings,
            request: SaveRequest,
        ): SaveOutcome {
            val token = SharedToken.parse(settings.variableIds.firstOrNull().orEmpty())
            val offered =
                token != null &&
                    variables.sharedOffers(room).any {
                        it.roomId == token.roomId && it.boxId == token.boxId && it.target == token.target
                    }
            val own = room.furni[request.boxId]?.let { variables.tokenOf(room, it) }
            return when {
                token == null || !offered -> {
                    SaveOutcome.Invalid(REFERENCE_VALID)
                }

                token.text != own && token.text in variables.referencedTokens(room) -> {
                    SaveOutcome.Invalid(REFERENCE_EXISTS)
                }

                else -> {
                    val name = cleaned(settings.text)
                    nameProblem(room, name, request.boxId) ?: SaveOutcome.Saved(settings.copy(text = name))
                }
            }
        }
    }

    private inner class Echo : BoxValidation {
        override fun check(
            room: Room,
            settings: BoxSettings,
        ): SaveOutcome = SaveOutcome.Saved(settings)

        override fun check(
            room: Room,
            settings: BoxSettings,
            request: SaveRequest,
        ): SaveOutcome {
            if (settings.text.isEmpty()) return SaveOutcome.Invalid(NAME_LENGTH)
            val source =
                variables.find(room, settings.variableIds.firstOrNull().orEmpty())
                    ?: return SaveOutcome.Invalid(PARAMS_MISSING)
            val sourceBox = room.furni[source.boxId]
            if (sourceBox != null && Interactions.of(sourceBox.definition) == RoomVariables.ECHO_BOX) {
                return SaveOutcome.Invalid(ECHO_OF_ECHO)
            }
            val name = cleaned(settings.text)
            return nameProblem(room, name, request.boxId) ?: SaveOutcome.Saved(settings.copy(text = name))
        }
    }

    private inner class Changed : BoxValidation {
        override fun check(
            room: Room,
            settings: BoxSettings,
        ): SaveOutcome {
            val picked = settings.picks.map { room.furni[it] }
            if (settings.ints.getOrElse(MODE) { 0 } != 0) {
                val codes = picked.map { it?.let(::codeOf) }
                return when {
                    codes.any { it in EXECUTION_BOXES } -> SaveOutcome.Invalid(REQUIRES_EXECUTION)
                    codes.any { it !in WATCHABLE } -> SaveOutcome.Invalid(MISSING_VARIABLE)
                    else -> SaveOutcome.Saved(settings)
                }
            }
            val id = settings.variableIds.firstOrNull().orEmpty()
            if (id.isEmpty() || id == RoomVariables.NONE) return SaveOutcome.Saved(settings)
            val variable = variables.find(room, id) ?: return SaveOutcome.Invalid(PARAMS_MISSING)
            return if (variable.entry.flags.requiresExecutionContext ||
                variable.entry.target == VariableTarget.CONTEXT
            ) {
                SaveOutcome.Invalid(REQUIRES_EXECUTION)
            } else {
                SaveOutcome.Saved(settings)
            }
        }

        private fun codeOf(furni: RoomFurni): String = Interactions.of(furni.definition)
    }

    /** "0" chooses nothing and saves; any other id must name a variable of the room. */
    private fun namesVariable(refusal: String): BoxValidation =
        BoxValidation { room, settings ->
            if (settings.variableIds.all { known(room, it) }) {
                SaveOutcome.Saved(settings)
            } else {
                SaveOutcome.Invalid(refusal)
            }
        }

    private fun known(
        room: Room,
        id: String,
    ): Boolean = id.isEmpty() || id == RoomVariables.NONE || variables.find(room, id) != null

    private companion object {
        const val MAX_NAME = 40
        const val MODE = 5
        val WHITESPACE = Regex("\\s")
        val NAME_SYNTAX = Regex("[A-Za-z0-9_]+")
        const val PERMANENT_LIMIT = "wiredfurni.error.variables.permanent_limit"
        const val NAME_LENGTH = "wiredfurni.error.variables.name_length"
        const val NAME_SYNTAX_ERROR = "wiredfurni.error.variables.name_syntax"
        const val NAME_UNIQ = "wiredfurni.error.variables.name_uniq"
        const val INVALID_AVAILABILITY = "wiredfurni.error.variables.invalid_availability"
        const val MISSING_VARIABLE = "wiredfurni.error.variables.missing_variable"
        const val REQUIRES_EXECUTION = "wiredfurni.error.variables.requires_execution_context"
        const val REFERENCE_VALID = "wiredfurni.error.variables.reference.valid"
        const val REFERENCE_EXISTS = "wiredfurni.error.variables.reference.already_exists"
        const val PARAMS_MISSING = "wiredfurni.params.variables.validation.missing_variable"
        const val ECHO_OF_ECHO = "wiredfurni.error.echo.self_reference"
        const val CONDITION_ERROR = "This condition cannot be saved with these settings."
        const val SELECTOR_ERROR = "This effect cannot be saved with these settings."
        val WATCHABLE = setOf(RoomVariables.USER_BOX, RoomVariables.ROOM_BOX, RoomVariables.REFERENCE_BOX)
        val EXECUTION_BOXES = setOf(RoomVariables.POINTER_BOX, RoomVariables.CONTEXT_BOX)

        val userAvailability: (Room, BoxSettings) -> SaveOutcome.Invalid? = { _, settings ->
            val availability = settings.ints.getOrElse(0) { Availability.WHILE_IN_ROOM }
            if (availability in setOf(Availability.WHILE_IN_ROOM, Availability.PERMANENT, Availability.SHARED)) {
                null
            } else {
                SaveOutcome.Invalid(INVALID_AVAILABILITY)
            }
        }

        val furniAvailability: (Room, BoxSettings) -> SaveOutcome.Invalid? = { _, settings ->
            val availability = settings.ints.getOrElse(1) { Availability.WHILE_ROOM_ACTIVE }
            if (availability == Availability.WHILE_ROOM_ACTIVE || availability == Availability.PERMANENT) {
                null
            } else {
                SaveOutcome.Invalid(INVALID_AVAILABILITY)
            }
        }

        val roomAvailability: (Room, BoxSettings) -> SaveOutcome.Invalid? = { _, settings ->
            val availability = settings.ints.getOrElse(0) { Availability.WHILE_ROOM_ACTIVE }
            if (availability in setOf(Availability.WHILE_ROOM_ACTIVE, Availability.PERMANENT, Availability.SHARED)) {
                null
            } else {
                SaveOutcome.Invalid(INVALID_AVAILABILITY)
            }
        }
    }
}
