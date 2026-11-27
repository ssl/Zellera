package zel.game.players

/**
 * What the measuring page or the client says about the browser and its computer:
 * `1-<id>-<gpu>-<scr>-<cpu>-<tz>-<fnt>-<cnv>-<aud>-<gl>-<ua>`, the id 16 hex characters and the others 8.
 * A piece of all zeros is one the browser does not give, one of all f's cannot be trusted to come out the same twice.
 */
class DeviceReading private constructor(
    private val text: String,
) {
    /** The ten pieces in the order of [NAMES]. */
    val pieces: List<String> = text.split('-').drop(1)

    /** Names of the pieces that differ; a piece either side does not give or cannot trust is skipped. */
    fun differingFrom(other: DeviceReading): List<String> =
        NAMES.indices
            .filter { compared(pieces[it]) && compared(other.pieces[it]) && pieces[it] != other.pieces[it] }
            .map(NAMES::get)

    fun sharesPieceWith(other: DeviceReading): Boolean =
        NAMES.indices.any { compared(pieces[it]) && compared(other.pieces[it]) }

    /** This reading with each piece it lacks taken from [other]; null when nothing usable is left. */
    fun filledFrom(other: DeviceReading?): DeviceReading? {
        val filled = pieces.mapIndexed { i, piece -> if (compared(piece)) piece else other?.pieces?.get(i) ?: piece }
        return DeviceReading("1-" + filled.joinToString("-")).takeIf { it.pieces.any(::compared) }
    }

    /** The piece called [name]; null when the browser does not give it or it cannot be trusted. */
    fun usable(name: String): String? = pieces[NAMES.indexOf(name)].takeIf(::compared)

    override fun toString(): String = text

    companion object {
        val NAMES = listOf("id", "gpu", "scr", "cpu", "tz", "fnt", "cnv", "aud", "gl", "ua")

        private val SHAPE = Regex("1-[0-9a-f]{16}(-[0-9a-f]{8}){9}")

        /** Null for anything that is not exactly a reading of version 1. */
        fun parse(text: String?): DeviceReading? = text?.takeIf(SHAPE::matches)?.let(::DeviceReading)

        private fun compared(piece: String): Boolean = piece.any { it != '0' } && piece.any { it != 'f' }
    }
}
