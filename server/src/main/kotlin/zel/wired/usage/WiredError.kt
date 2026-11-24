package zel.wired.usage

enum class WiredError {
    EXECUTION_CAP,
    DELAYED_EVENTS_CAP,
    EXECUTOR_OVERLOAD,
    MARKED_AS_HEAVY,
    KILLED,
    RECURSION_TIMEOUT,
    TOO_MANY_VARIABLES,
    TRANSACTION_FAILURE,
    PLACEMENT_FAILURE,
    VARIABLE_FX_LIMIT,
    GROUP_INVITATION_LIMIT,
    ;

    /** The id the row shows: VARIABLE_FX_LIMIT shares PLACEMENT_FAILURE's, the rows after it count on from there. */
    val wireId: Int get() = if (this >= VARIABLE_FX_LIMIT) ordinal - 1 else ordinal

    val category: String get() = if (this == MARKED_AS_HEAVY || this == GROUP_INVITATION_LIMIT) "WARNING" else "ERROR"
}
