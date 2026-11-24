package zel.wired.variable.box

import zel.wired.box.BoxKind
import zel.wired.box.BoxSettings
import zel.wired.box.BoxType
import zel.wired.box.WiredBox
import zel.wired.form.DialogSpecs
import zel.wired.variable.RoomVariables

class VariableBoxes(
    variables: RoomVariables,
) {
    val types: List<BoxType<WiredBox>> =
        DEFINITIONS.map { type(it, BoxKind.Variable, ::DefinitionBox) } +
            listOf(
                type("wf_trg_var_changed", BoxKind.Trigger, ::VariableChanged),
                type("wf_act_give_var", BoxKind.Effect) { id, s -> GiveVariable(id, s, variables) },
                type("wf_act_remove_var", BoxKind.Effect) { id, s -> RemoveVariable(id, s, variables) },
                type("wf_act_change_var_val", BoxKind.Effect) { id, s -> ChangeVariableValue(id, s, variables) },
                type("wf_cnd_has_var", BoxKind.Condition) { id, s -> HasVariable(id, s, negative = false, variables) },
                type(
                    "wf_cnd_neg_has_var",
                    BoxKind.Condition,
                ) { id, s -> HasVariable(id, s, negative = true, variables) },
                type("wf_cnd_var_val_match", BoxKind.Condition) { id, s -> VariableValueMatch(id, s, variables) },
                type("wf_cnd_var_age_match", BoxKind.Condition) { id, s ->
                    VariableAgeMatch(id, s, variables, variables.clock)
                },
                type("wf_slc_furni_with_var", BoxKind.Selector) { id, s -> FurniWithVariable(id, s, variables) },
                type("wf_slc_users_with_var", BoxKind.Selector) { id, s -> UsersWithVariable(id, s, variables) },
                type("wf_xtra_filter_furni_by_var", BoxKind.Addon) { id, s -> FilterByVariable(id, s, variables) },
                type("wf_xtra_filter_users_by_var", BoxKind.Addon) { id, s -> FilterByVariable(id, s, variables) },
            )

    private fun type(
        code: String,
        kind: BoxKind,
        create: (Int, BoxSettings) -> WiredBox,
    ): BoxType<WiredBox> = DialogSpecs.boxType(code, kind, create)

    companion object {
        val DEFINITIONS =
            listOf(
                RoomVariables.USER_BOX,
                RoomVariables.FURNI_BOX,
                RoomVariables.ROOM_BOX,
                RoomVariables.CONTEXT_BOX,
                RoomVariables.POINTER_BOX,
                RoomVariables.REFERENCE_BOX,
                RoomVariables.ECHO_BOX,
            )
    }
}
