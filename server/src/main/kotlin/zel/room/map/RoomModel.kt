package zel.room.map

data class RoomModel(
    val name: String,
    val map: String,
    val door: Door,
) {
    val smallScale: Boolean get() = smallScale(name)

    companion object {
        private val LARGE_TILE_MODELS = ('a'..'h').map { "model_$it" }.toSet()

        fun smallScale(modelName: String): Boolean = modelName.lowercase() !in LARGE_TILE_MODELS
    }
}
