package zel.game.pets

import org.slf4j.LoggerFactory
import zel.db.allOrNothing
import zel.game.items.Item
import zel.protocol.BodyPart
import zel.protocol.generated.PacketWriter
import java.sql.Connection
import java.sql.SQLException
import java.util.Locale

open class Pet(
    val pets: PetsModule,
    row: PetRow,
) : BodyPart {
    val services: PetServices = pets.services

    var id: Int = row.id
    val userId: Int = row.ownerId
    private val type: Int = row.type
    val kind: PetKind? =
        pets.catalog.kind(row.type).also { if (it == null) LOGGER.warn("Pet {} has unknown type {}", row.id, row.type) }
    var race: Int = row.race
    val color: String? = row.color
    var created: Int = row.bornAt
    var level: Int = PetLevels.levelFor(row.stats.experience)

    /** Shared mutable state: changed on the owning room's thread, read by the inventory's save. */
    @Volatile
    var stats: PetStats = row.stats

    @Volatile
    var hasUnsavedChanges = false
        private set

    private var ownName: String = row.name.orEmpty()

    open var name: String
        get() = ownName
        set(value) {
            ownName = value
            markChanged()
        }

    open var happiness: Int
        get() = stats.happiness
        set(value) = change { copy(happiness = value) }

    open var experience: Int
        get() = stats.experience
        set(value) = change { copy(experience = value) }

    open var energy: Int
        get() = stats.energy
        set(value) = change { copy(energy = value) }

    val respect: Int get() = stats.respect

    var thirst: Int
        get() = stats.thirst
        set(value) = change { copy(thirst = value) }

    open val breedId: Int get() = race

    open val maxEnergy: Int get() = PetLevels.maxEnergyAt(level)

    val figureColor: String get() = figureColor(type, color ?: WHITE)

    protected open val listedColor: String? get() = figureColor

    fun gainEnergy(amount: Int) = change { withEnergy(amount, PetLevels.maxEnergyAt(level)) }

    fun eat(food: Item) = change { withBite(PetFood.biteEnergy(food)).withHappiness(PetFood.biteHappiness(food)) }

    val isHungry: Boolean get() = PetFood.wantsFood(stats.energy, PetLevels.maxEnergyAt(level))

    fun cheer(amount: Int) = change { withHappiness(amount) }

    fun receiveRespect() = change { respected() }

    fun changeThirst(amount: Int) = change { withThirst(amount) }

    private fun change(next: PetStats.() -> PetStats) {
        stats = stats.next()
        markChanged()
    }

    fun markChanged() {
        hasUnsavedChanges = true
    }

    /** Saves the pet's rows as one transaction, so a failed second statement never leaves a pet row behind. */
    fun saveIfChanged() {
        if (!hasUnsavedChanges) return
        val storedId = id
        try {
            services.dataSource.connection.use { connection -> connection.allOrNothing { save(connection) } }
        } catch (e: SQLException) {
            id = storedId
            markChanged()
            LOGGER.error("Pet {} could not be saved", id, e)
        }
    }

    /** Inserts a new pet or updates its row; the flag clears before the snapshot, so a later change saves next time. */
    @Throws(SQLException::class)
    fun save(connection: Connection) {
        val storedId = id
        hasUnsavedChanges = false
        try {
            val row = currentRow()
            if (storedId == 0) id = PetRows.insert(connection, row) else PetRows.update(connection, row)
            storeExtras(connection)
        } catch (failure: SQLException) {
            id = storedId
            hasUnsavedChanges = true
            throw failure
        }
    }

    protected open fun storeExtras(connection: Connection) = Unit

    private fun currentRow(): PetRow = PetRow(id, userId, name, type, race, color, created, stats)

    override fun writeTo(body: PacketWriter) {
        body
            .int(id)
            .string(name)
            .int(kind?.type ?: type)
            .int(race)
            .string(listedColor ?: figureColor)
            .int(breedId)
        writeParts(body)
        body.int(level)
    }

    /** The custom parts list: its count, then part id, style and colour each. */
    open fun writeParts(body: PacketWriter) {
        body.int(0)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(Pet::class.java)
        const val WHITE = "ffffff"
        private const val LAST_CLASSIC_TYPE = 7

        fun newRow(
            pets: PetsModule,
            type: Int,
            race: Int,
            color: String?,
            name: String?,
            userId: Int,
        ): PetRow =
            PetRow(
                id = 0,
                ownerId = userId,
                name = name,
                type = type,
                race = race,
                color = figureColor(type, color ?: WHITE),
                bornAt = pets.services.now(),
                stats = PetStats.NEW,
            )

        /** Types up to [LAST_CLASSIC_TYPE] write their colour in upper case, later ones in lower case. */
        fun figureColor(
            type: Int,
            color: String,
        ): String = if (type <= LAST_CLASSIC_TYPE) color.uppercase(Locale.ROOT) else color.lowercase(Locale.ROOT)
    }
}
