package zel.game.icons

class IconRejected(
    val code: Int,
) : Exception("icon action refused with code $code") {
    companion object {
        const val UNKNOWN = 1
        const val UNAVAILABLE = 4
    }
}
