package zel.room.unit.path

sealed interface Passage {
    data object SkateRail : Passage

    data class Obstacle(
        val middle: Boolean,
    ) : Passage

    data class Transit(
        val furniId: Int,
    ) : Passage

    data class Rented(
        val renterId: Int,
    ) : Passage

    data class Nest(
        val furniId: Int,
    ) : Passage
}
