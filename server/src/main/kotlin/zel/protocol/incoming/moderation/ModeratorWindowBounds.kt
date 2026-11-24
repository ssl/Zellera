package zel.protocol.incoming.moderation

internal object ModeratorWindowBounds {
    private const val MAX_WINDOW_COORDINATE = 10000

    fun isSaneCoordinate(value: Int) = value in -MAX_WINDOW_COORDINATE..MAX_WINDOW_COORDINATE

    fun isSaneSize(value: Int) = value in 0..MAX_WINDOW_COORDINATE
}
