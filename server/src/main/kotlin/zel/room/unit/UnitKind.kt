package zel.room.unit

sealed interface UnitKind {
    data class Player(
        val playerId: Int,
    ) : UnitKind

    data class Bot(
        val botId: Int,
    ) : UnitKind

    data class Pet(
        val petId: Int,
    ) : UnitKind
}
