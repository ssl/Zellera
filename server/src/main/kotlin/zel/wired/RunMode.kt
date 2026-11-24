package zel.wired

sealed interface RunMode {
    data object Event : RunMode

    data class Call(
        val negative: Boolean,
    ) : RunMode

    data object Direct : RunMode
}
