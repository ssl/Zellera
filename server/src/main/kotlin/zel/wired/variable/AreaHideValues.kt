package zel.wired.variable

internal object AreaHideValues {
    const val STATE = "state"
    const val ROOT_X = "rootX"
    const val ROOT_Y = "rootY"
    const val WIDTH = "width"
    const val LENGTH = "length"
    const val INVISIBLE = "invisibility"
    const val WALL_ITEMS = "wallItems"
    const val INVERT = "invert"
    val DEFAULTS =
        linkedMapOf(
            STATE to "0",
            ROOT_X to "0",
            ROOT_Y to "0",
            WIDTH to "0",
            LENGTH to "0",
            INVISIBLE to "0",
            WALL_ITEMS to "1",
            INVERT to "0",
        )
}
