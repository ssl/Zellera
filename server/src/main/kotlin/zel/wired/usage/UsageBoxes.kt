package zel.wired.usage

import zel.wired.box.BoxKind
import zel.wired.box.BoxType
import zel.wired.box.WiredBox
import zel.wired.form.DialogSpecs

/** Forms come from the dialog table. */
object UsageBoxes {
    val types: List<BoxType<WiredBox>> =
        listOf(
            DialogSpecs.boxType("wf_xtra_execution_limit", BoxKind.Addon, ::ExecutionLimit),
        )
}
