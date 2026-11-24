package zel.wired.box

interface ConditionMode : AddonBox {
    /** Furni ids of the condition boxes this add-on applies to; empty: every condition of its own stack. */
    val appliesTo: List<Int> get() = emptyList()

    fun passes(results: List<Boolean>): Boolean
}
