package zel.game.pets

import zel.db.first
import zel.db.orFallback
import zel.db.write
import zel.game.items.PetProducts
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap

class PetCatalog(
    private val services: PetServices,
) {
    private val definitions =
        PetDefinitions.load(services.dataSource, services::random)
    private val kinds = ConcurrentHashMap(definitions.kinds)
    private val typesByProduct =
        definitions.kinds.values
            .filter { it.productName.isNotBlank() }
            .associate { it.productName.lowercase() to it.type }

    init {
        PetProducts.install(typesByProduct.keys)
        PetBoxes.install(definitions.boxes) { definitions.kinds[it]?.behaviour ?: PetBehaviour.PLAIN }
    }

    fun behaviourOf(type: Int): PetBehaviour = kinds[type]?.behaviour ?: PetBehaviour.PLAIN

    /** The first type of [behaviour], null when the content has none. */
    fun typeOf(behaviour: PetBehaviour): Int? =
        kinds.values.filter { it.behaviour == behaviour }.minOfOrNull { it.type }

    fun kind(type: Int): PetKind? = kinds[type] ?: synchronized(kinds) { kinds[type] ?: addMissingKind(type) }

    fun hasKind(type: Int): Boolean = kinds.containsKey(type)

    private fun addMissingKind(type: Int): PetKind? {
        LOGGER.error("Pet type {} has no pet_types row; adding one", type)
        return LOGGER.orFallback("adding pet type $type", null) {
            services.dataSource.connection.use { connection ->
                insertDefaults(connection, type)
                readKind(connection, type)?.also { kinds[type] = it }
            }
        }
    }

    private fun insertDefaults(
        connection: Connection,
        type: Int,
    ) {
        connection.write(INSERT_KIND) {
            int(type)
            string("pet_type_$type")
            intOrNull(PetKind.NO_OFFSPRING, PetKind.NO_OFFSPRING)
        }
    }

    private fun readKind(
        connection: Connection,
        type: Int,
    ): PetKind? =
        connection.first("SELECT * FROM pet_types WHERE id = ? LIMIT 1", { int(type) }) { definitions.kindFrom(it) }

    fun palettesOf(petFurniName: String): List<PetPalette>? {
        val type = typeSoldBy(petFurniName)
        if (type == null) LOGGER.warn("no pet palettes for {}: not a pet product name", petFurniName)
        return type?.let(::palettesOf)
    }

    fun palettesOf(type: Int): List<PetPalette>? = definitions.palettesByType[type]

    fun typeSoldBy(productName: String): Int? = typesByProduct[productName.lowercase()]

    fun isClubOnlyBreed(
        type: Int,
        color: Int,
    ): Boolean =
        definitions.palettesByType[type]
            ?.firstOrNull { it.breedId == color || it.paletteId == color }
            ?.membersOnly == true

    /** The breeds a baby of [type] is born in, by tier; empty for a type no nest gives. */
    fun babyBreeds(type: Int): Map<BabyTier, List<Int>> = definitions.babyBreeds[type].orEmpty()

    fun tierOf(baby: Pet): BabyTier {
        val breeds = babyBreeds(baby.kind?.type ?: PetKind.NO_OFFSPRING)
        return breeds.entries.firstOrNull { baby.race in it.value }?.key ?: BabyTier.COMMON
    }

    fun packageFor(
        type: Int,
        palette: Int,
        color: String,
    ): PetPackage? =
        definitions.packagesByName.values.firstOrNull {
            it.petType == type && it.palette == palette && it.color.equals(color, ignoreCase = true)
        }

    fun packageNamed(itemName: String): PetPackage? = definitions.packagesByName[itemName]

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(PetCatalog::class.java)
        private const val INSERT_KIND = "INSERT INTO pet_types (id, name, offspring_type_id) VALUES (?, ?, ?)"
    }
}
