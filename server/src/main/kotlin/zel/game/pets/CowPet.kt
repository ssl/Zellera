package zel.game.pets

import java.sql.Connection
import java.sql.ResultSet
import java.util.Random

class CowPet :
    Pet,
    PetLook {
    val parts: String

    constructor(pets: PetsModule, set: ResultSet) : super(pets, PetRow.read(set)) {
        val stored = set.getString("look_parts")
        parts = if (stored.isNullOrBlank()) randomParts(Random(id.toLong())) else stored
    }

    constructor(
        pets: PetsModule,
        type: Int,
        race: Int,
        color: String?,
        name: String?,
        userId: Int,
        parts: String,
    ) : super(pets, newRow(pets, type, race, color, name, userId)) {
        this.parts = parts
    }

    override val look: String
        get() = "${kind?.type ?: -1} $race $figureColor 3 $parts"

    override fun storeExtras(connection: Connection) = PetRows.saveLook(connection, id, parts)

    companion object {
        val BODY_COLORS = intArrayOf(2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 28, 29, 31, 32)

        val HORNS: Map<Int, IntArray> =
            mapOf(
                201 to intArrayOf(20, 21, 22, 24, 27, 30),
                202 to intArrayOf(3, 27),
                203 to intArrayOf(23, 25, 26, 27),
                204 to intArrayOf(20, 24, 27),
            )

        private val TINTED_HORNS = HORNS.flatMap { (style, tints) -> tints.map { style to it } }

        fun randomParts(random: Random): String {
            val body = BODY_COLORS[random.nextInt(BODY_COLORS.size)]
            val (horns, tint) = TINTED_HORNS[random.nextInt(TINTED_HORNS.size)]
            return "0 -1 $body 1 -1 $body 2 $horns $tint"
        }
    }
}
