package zel.game.effects

import java.util.concurrent.atomic.AtomicReference

/** The avatar effect each server role puts on a unit, as the content defines them; a role with no row is no effect. */
object EffectRoles {
    const val FROZEN = "freeze.frozen"
    const val TELEPORT = "transport.normal"
    const val TELEPORT_FAST = "transport.fast"
    const val TELEPORT_ROOM = "transport.room"
    const val WATER = "plant.watered"
    const val CANNON = "launcher.hit"
    const val RIDING = "riding"
    const val AMBASSADOR = "ambassador"
    const val MOONWALK = "moonwalk"
    const val DANCE_FLOOR = "dance_floor"

    const val TEAM = "team."
    const val TILE_CLAIM_TEAM = "tileclaim.team."
    const val FREEZE_TEAM = "freeze.team."
    const val FREEZE_SHIELD = "freeze.shield."

    private val ids = AtomicReference<Map<String, Int>>(emptyMap())

    fun install(roles: Map<String, Int>) {
        ids.set(roles.mapKeys { it.key.lowercase() })
    }

    fun of(role: String): Int = ids.get()[role.lowercase()] ?: 0
}
