package zel.wired.box.pattern

import zel.wired.box.BoxKind
import zel.wired.box.BoxType
import zel.wired.box.WiredBox
import zel.wired.form.DialogSpecs

/** Forms come from the dialog table. */
object PatternBoxes {
    val types: List<BoxType<WiredBox>> =
        listOf(
            DialogSpecs.boxType("wf_trg_walks_on_furni", BoxKind.Trigger, ::WalksOnFurni),
            DialogSpecs.boxType("wf_act_toggle_state", BoxKind.Effect, ::ToggleState),
            DialogSpecs.boxType("wf_cnd_user_count_in", BoxKind.Condition, ::UserCountIn),
        )
}
