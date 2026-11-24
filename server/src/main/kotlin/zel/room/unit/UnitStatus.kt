package zel.room.unit

object UnitStatus {
    const val MOVE = "mv"
    const val SIT = "sit"
    const val LAY = "lay"
    const val FLAT_CONTROL = "flatctrl"
    const val SIGN = "sign"
    const val SLEEP = "slp"
    const val SWIM = "swim"

    /** Postures a step ends: a walking unit loses them before it moves. */
    val ENDED_BY_STEP: Set<String> = setOf(SIT, LAY, "std", "beg", "ded", "jmp", "pla", SLEEP, "wng", "wf")
}
