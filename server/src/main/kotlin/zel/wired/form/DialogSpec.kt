package zel.wired.form

import zel.wired.box.BoxForm
import zel.wired.box.BoxKind

data class DialogSpec(
    val code: String,
    val kind: BoxKind,
    val form: BoxForm,
    val advanced: Boolean = true,
    val quantifierType: Int = 0,
    val invert: Boolean = false,
    /** Context entry types, in order (0 variable hash, 7 text placeholders, ...). */
    val contexts: List<Int> = emptyList(),
    val closingInts: List<Int> = form.fresh.ints,
)
