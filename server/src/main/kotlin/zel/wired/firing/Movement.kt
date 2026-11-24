package zel.wired.firing

sealed interface Movement {
    val key: Key

    fun then(next: Movement): Movement

    data class Key(
        val type: Int,
        val id: Int,
    )

    data class Spot(
        val x: Int,
        val y: Int,
        val z: String,
    )

    data class UnitMove(
        val unitIndex: Int,
        val from: Spot,
        val to: Spot,
        val moveType: Int,
        val duration: Int,
        val bodyDirection: Int,
        val headDirection: Int,
    ) : Movement {
        override val key = Key(TYPE_UNIT, unitIndex)

        override fun then(next: Movement): Movement = if (next is UnitMove) next.copy(from = from) else next

        companion object {
            const val WALK = 0
            const val SLIDE = 1
        }
    }

    data class FurniMove(
        val furniId: Int,
        val from: Spot,
        val to: Spot,
        val rotation: Int,
        val duration: Int,
    ) : Movement {
        override val key = Key(TYPE_FURNI, furniId)

        override fun then(next: Movement): Movement = if (next is FurniMove) next.copy(from = from) else next
    }

    data class WallSpot(
        val wallX: Int,
        val wallY: Int,
        val localX: Int,
        val localY: Int,
    )

    data class WallMove(
        val furniId: Int,
        val rightSide: Boolean,
        val from: WallSpot,
        val to: WallSpot,
        val duration: Int,
    ) : Movement {
        override val key = Key(TYPE_WALL, furniId)

        override fun then(next: Movement): Movement = if (next is WallMove) next.copy(from = from) else next
    }

    data class UnitTurn(
        val unitIndex: Int,
        val direction: Int,
    ) : Movement {
        override val key = Key(TYPE_TURN, unitIndex)

        override fun then(next: Movement): Movement = next
    }

    companion object {
        const val TYPE_UNIT = 0
        const val TYPE_FURNI = 1
        const val TYPE_WALL = 2
        const val TYPE_TURN = 3
    }
}
