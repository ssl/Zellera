@file:Suppress("MagicNumber")

package zel.game.pets

import zel.game.items.Item
import zel.protocol.generated.PacketWriter
import java.sql.Connection
import java.sql.ResultSet

class HorsePet : Pet {
    /** -1 is the coat's own. */
    data class Mane(
        val style: Int,
        val tint: Int,
    )

    var mane: Mane
    private var saddleOn = false
    private var ridersWelcome = false

    /** The saddle furni it wears or last wore; 0 when it was never saddled. */
    var saddleDefinitionId = 0

    var saddled: Boolean
        get() = saddleOn
        set(value) {
            saddleOn = value
            markChanged()
        }

    var openToRiders: Boolean
        get() = ridersWelcome
        set(value) {
            ridersWelcome = value
            markChanged()
        }

    constructor(pets: PetsModule, set: ResultSet) : super(pets, PetRow.read(set)) {
        val hasRow = set.getObject("mane_style") != null
        mane = if (hasRow) Mane(set.getInt("mane_style"), set.getInt("mane_tint")) else Mane(-1, FIRST_MANE_TINT)
        saddleDefinitionId = set.getInt("saddle_definition_id")
        saddleOn = saddleDefinitionId > 0
        ridersWelcome = set.getBoolean("open_to_riders")
    }

    constructor(
        pets: PetsModule,
        kind: Int,
        coat: Int,
        colour: String?,
        name: String?,
        ownerId: Int,
    ) : super(pets, newRow(pets, kind, coat, colour, name, ownerId)) {
        mane = Mane(-1, FIRST_MANE_TINT)
    }

    override val breedId: Int
        get() = breedOfCoat(race)

    val saddleFigurePart: Int
        get() {
            if (saddleDefinitionId <= 0) return SADDLE_1_PART
            return saddleFigurePartOf(services.items().getItem(saddleDefinitionId))
        }

    override fun storeExtras(connection: Connection) {
        val saddle = if (saddleOn) saddleDefinitionId else 0
        PetRows.saveHorse(connection, PetRows.HorseRow(id, mane.style, mane.tint, saddle, ridersWelcome))
    }

    data class FigurePart(
        val slot: Int,
        val style: Int,
        val tint: Int,
    )

    /** The mane twice (slots 2 and 3), then the saddle (slot 4) when it is on. */
    val figureParts: List<FigurePart>
        get() =
            MANE_SLOTS.map { FigurePart(it, mane.style, mane.tint) } +
                listOfNotNull(FigurePart(SADDLE_SLOT, saddleFigurePart, 0).takeIf { saddleOn })

    override fun writeParts(body: PacketWriter) {
        val parts = figureParts
        body.int(parts.size)
        parts.forEach { body.int(it.slot).int(it.style).int(it.tint) }
    }

    companion object {
        const val TYPE = 15

        const val FIRST_MANE_TINT = 1

        /** The figure part of horse_saddle1, also the one of a saddle furni that is not known. */
        const val SADDLE_1_PART = 9

        private const val SADDLE_2_PART = 10
        private val MANE_SLOTS = listOf(2, 3)
        private const val SADDLE_SLOT = 4

        private val SELLABLE_COATS = 2..49
        private val SPECIAL_COATS = 77..80
        private const val BREEDS = 4
        private const val LONE_COAT = 61

        fun breedOfCoat(coat: Int): Int =
            when (coat) {
                in SELLABLE_COATS -> (coat - SELLABLE_COATS.first) % BREEDS + 1
                in SPECIAL_COATS -> coat - SPECIAL_COATS.first + 1
                LONE_COAT -> 1
                else -> 0
            }

        /** The saddle's figure part: declared in the furni's params as "15 4 <part>", else by its name. */
        fun saddleFigurePartOf(saddle: Item?): Int {
            if (saddle == null) return SADDLE_1_PART
            val words = saddle.params.trim().split(Regex("\\s+"))
            val declared = if (words.size >= 3 && words[0] == "15" && words[1] == "4") words[2].toIntOrNull() else null
            if (declared != null) return declared
            return if ("horse_saddle2".equals(saddle.name, ignoreCase = true)) SADDLE_2_PART else SADDLE_1_PART
        }
    }
}
