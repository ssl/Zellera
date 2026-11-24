package zel.wired.box

object BoxCodes {
    private val PREFIXES: List<Pair<String, BoxKind>> =
        listOf(
            "wf_trg_" to BoxKind.Trigger,
            "wf_cnd_" to BoxKind.Condition,
            "wf_act_" to BoxKind.Effect,
            "wf_slc_" to BoxKind.Selector,
            "wf_xtra_" to BoxKind.Addon,
            "wf_var_" to BoxKind.Variable,
        )

    const val SCAN_CHEST = "wf_xtra_scan_chest_furni_by_type"

    fun kindOf(code: String): BoxKind? =
        if (code == SCAN_CHEST) BoxKind.Selector else PREFIXES.firstOrNull { code.startsWith(it.first) }?.second

    fun isBox(code: String): Boolean = kindOf(code) != null
}
