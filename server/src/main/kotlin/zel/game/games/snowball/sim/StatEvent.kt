package zel.game.games.snowball.sim

sealed interface StatEvent {
    data class Hit(
        val thrower: Human,
        val target: Human,
        val knockdown: Boolean,
    ) : StatEvent

    data class FriendlyHit(
        val thrower: Human,
        val target: Human,
    ) : StatEvent

    data class Thrown(
        val thrower: Human,
    ) : StatEvent

    data class Made(
        val human: Human,
    ) : StatEvent

    data class PickedUp(
        val human: Human,
        val giver: SnowballGiver,
    ) : StatEvent

    data class TreeHit(
        val thrower: Human,
        val tree: Tree,
    ) : StatEvent
}
