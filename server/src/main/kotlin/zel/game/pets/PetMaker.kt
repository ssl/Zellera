package zel.game.pets

import zel.game.items.Item
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import kotlin.random.asKotlinRandom

class PetMaker(
    private val pets: PetsModule,
) {
    private val services get() = pets.services

    @Throws(SQLException::class)
    fun fromRow(row: ResultSet): Pet =
        when (val type = row.getInt("type_id")) {
            HorsePet.TYPE -> {
                HorsePet(pets, row)
            }

            PetKind.MONSTERPLANT_TYPE -> {
                MonsterPlant(pets, row)
            }

            else -> {
                when (pets.catalog.behaviourOf(type)) {
                    PetBehaviour.GNOME, PetBehaviour.LEPRECHAUN -> GnomePet(pets, row)
                    PetBehaviour.COW -> CowPet(pets, row)
                    PetBehaviour.PLAIN -> Pet(pets, PetRow.read(row))
                }
            }
        }

    /** A bought pet, saved in the buyer's transaction; null for a monster plant or an unknown type. */
    @Throws(SQLException::class)
    fun boughtOn(
        connection: Connection,
        item: Item,
        name: String,
        race: String,
        color: String,
        ownerId: Int,
    ): Pet? {
        val type = pets.catalog.typeSoldBy(item.name) ?: return null
        if (!pets.catalog.hasKind(type) || type == PetKind.MONSTERPLANT_TYPE) return null
        val pet = ownBreed(type, race.toInt(), color, name, ownerId)
        pet.markChanged()
        pet.save(connection)
        return pet
    }

    @Throws(SQLException::class)
    fun breedOn(
        connection: Connection,
        type: Int,
        race: Int,
        name: String,
        ownerId: Int,
    ): Pet? {
        if (!pets.catalog.hasKind(type)) return null
        val pet = plainOrCow(type, race, Pet.figureColor(type, "FFFFFF"), name, ownerId)
        pet.markChanged()
        pet.save(connection)
        return pet
    }

    fun ofRandomPalette(
        type: Int,
        name: String,
        ownerId: Int,
    ): Pet? {
        val palette = pets.catalog.palettesOf(type)?.randomOrNull(services.random().asKotlinRandom()) ?: return null
        return ofBreed(type, palette.breedId, name, ownerId)
    }

    fun ofBreed(
        type: Int,
        race: Int,
        name: String,
        ownerId: Int,
        color: String = Pet.figureColor(type, "FFFFFF"),
    ): Pet? {
        if (!pets.catalog.hasKind(type)) return null
        return saved(plainOrCow(type, race, color, name, ownerId))
    }

    fun plant(
        ownerId: Int,
        rarity: Int? = null,
    ): MonsterPlant {
        val random = services.random()
        val (shape, hue) = rarity?.let { PlantRolls.ofRarity(it, random) } ?: PlantRolls.commonLeaning(random)
        val feature = { random.nextInt(PLANT_FEATURES) + 1 }
        val colour = { random.nextInt(PLANT_COLOURS) }
        return saved(
            MonsterPlant(pets, ownerId, shape, hue, feature(), colour(), feature(), colour(), feature(), colour()),
        )
    }

    fun gnome(
        name: String,
        ownerId: Int,
    ): Pet? {
        val type = pets.catalog.typeOf(PetBehaviour.GNOME) ?: return null
        val random = services.random()
        val parts = GnomePet.randomGnomeParts(random)
        return saved(GnomePet(pets, type, GnomePet.randomPalette(random), name, ownerId, parts))
    }

    fun leprechaun(
        name: String,
        ownerId: Int,
    ): Pet? {
        val type = pets.catalog.typeOf(PetBehaviour.LEPRECHAUN) ?: return null
        return saved(
            GnomePet(
                pets,
                type,
                GnomePet.randomPalette(services.random()),
                name,
                ownerId,
                GnomePet.LEPRECHAUN_PARTS,
            ),
        )
    }

    private fun ownBreed(
        type: Int,
        race: Int,
        color: String,
        name: String,
        ownerId: Int,
    ): Pet =
        if (type == HorsePet.TYPE) {
            HorsePet(pets, type, race, color, name, ownerId)
        } else {
            plainOrCow(type, race, color, name, ownerId)
        }

    internal fun plainOrCow(
        type: Int,
        race: Int,
        color: String,
        name: String,
        ownerId: Int,
    ): Pet =
        if (pets.catalog.behaviourOf(type) == PetBehaviour.COW) {
            CowPet(pets, type, race, color, name, ownerId, CowPet.randomParts(services.random()))
        } else {
            Pet(pets, Pet.newRow(pets, type, race, color, name, ownerId))
        }

    private fun <T : Pet> saved(pet: T): T {
        pet.markChanged()
        pet.saveIfChanged()
        return pet
    }

    private companion object {
        const val PLANT_FEATURES = 12
        const val PLANT_COLOURS = 11
    }
}
