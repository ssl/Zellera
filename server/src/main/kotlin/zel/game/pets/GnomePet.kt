package zel.game.pets

import java.sql.Connection
import java.sql.ResultSet
import java.util.Random

class GnomePet :
    Pet,
    PetLook {
    private val figureParts: String

    constructor(pets: PetsModule, set: ResultSet) : super(pets, PetRow.read(set)) {
        figureParts = set.getString("look_parts")
    }

    constructor(pets: PetsModule, type: Int, palette: Int, name: String?, ownerId: Int, figureParts: String) :
        super(pets, newRow(pets, type, palette, WHITE, name, ownerId)) {
        this.figureParts = figureParts
    }

    override val breedId: Int
        get() = 0

    override val listedColor: String?
        get() = color

    override val look: String
        get() = "${kind?.type ?: -1} $race $WHITE $figureParts"

    override fun storeExtras(connection: Connection) = PetRows.saveLook(connection, id, figureParts)

    private class Part(
        val style: Int,
        vararg val tints: Int,
    )

    companion object {
        const val LEPRECHAUN_PARTS = "5 0 -1 6 1 102 15 2 201 19 3 302 23 4 401 27"

        /** The palettes a gnome or leprechaun figure carries; they leave its parts as they are. */
        val PALETTES = intArrayOf(0, 1, 6, 7, 28, 29, 30)

        private val SKINS = intArrayOf(0, 1, 6, 7, 28, 29, 30)
        private val HATS = listOf(Part(101, 2, 12, 13), Part(102, 2, 8, 12, 13, 14, 15, 31))
        private val BODIES = listOf(Part(201, 3, 11, 16, 17, 18, 19, 32))
        private val BEARDS = listOf(Part(301, 4, 10, 22), Part(302, 4, 10, 21, 23, 33), Part(303, 4))
        private val CLOTHES = listOf(Part(401, 5, 9, 24, 25, 26, 27, 34), Part(402, 5))

        fun randomPalette(random: Random): Int = PALETTES[random.nextInt(PALETTES.size)]

        /** Hat, body, beard and clothes: figure slots 1 to 4. */
        private val DRESS = listOf(HATS, BODIES, BEARDS, CLOTHES)

        fun randomGnomeParts(random: Random): String {
            val dress =
                DRESS.mapIndexed { index, parts ->
                    val tinted = parts.flatMap { part -> part.tints.map { part.style to it } }
                    val (style, tint) = tinted[random.nextInt(tinted.size)]
                    "${index + 1} $style $tint"
                }
            return "5 0 -1 ${SKINS[random.nextInt(SKINS.size)]} ${dress.joinToString(" ")}"
        }
    }
}
