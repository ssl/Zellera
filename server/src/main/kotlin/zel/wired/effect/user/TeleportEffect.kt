package zel.wired.effect.user

/**
 * Compared by identity, so a later teleport's ending replaces it. [offTick]: the room tick that gives [before]
 * back, set by the trip's first landing ([afterLanding] ticks after it).
 */
class TeleportEffect(
    var before: Int,
    val effect: Int,
    val afterLanding: Int,
) {
    var offTick: Long? = null
}
