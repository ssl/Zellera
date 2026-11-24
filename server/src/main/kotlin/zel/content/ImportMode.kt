package zel.content

internal enum class ImportMode {
    REPLACE,

    MERGE,

    ROOMS,

    /** Matched to the game's players by name; no row is cleared. */
    PLAYERS,

    /** Written into an empty table only: once the game has rows of its own, the pack's are left out. */
    SEED,
}
