package zel.game.camera

data class PhotoFrame(
    val planes: List<Plane>,
    val sprites: List<Sprite>,
    val filters: List<FilterUse>,
    val roomId: Int,
    val zoom: Int,
) {
    data class Point(
        val x: Double,
        val y: Double,
    )

    data class Mask(
        val name: String,
        val x: Double,
        val y: Double,
        val flipH: Boolean,
        val flipV: Boolean,
    )

    data class Plane(
        val z: Double,
        val corners: List<Point>,
        val color: Int,
        val bottomAligned: Boolean,
        val textures: List<String>,
        val masks: List<Mask>,
    )

    data class Sprite(
        val name: String,
        val url: Boolean,
        val x: Double,
        val y: Double,
        val z: Double,
        val color: Int,
        val alpha: Int,
        val flipH: Boolean,
        val blendMode: String?,
        val width: Int,
        val height: Int,
        val skew: Double,
        val frame: Boolean,
        val palette: String?,
    )

    data class FilterUse(
        val name: String,
        val alpha: Int,
    )

    companion object {
        internal const val MAX_PLANES = 1024
        internal const val MAX_SPRITES = 4096
        internal const val MAX_FILTERS = 16
        internal const val MAX_MASKS = 64
        internal const val MAX_COLUMNS = 64
        internal const val MAX_COORDINATE = 20000.0

        fun parse(json: String): PhotoFrame = PhotoFrameReader.read(json)
    }
}
