package zel.game.players.inventory

import zel.db.SqlFailure
import zel.game.pets.Pet
import zel.game.pets.PetRows
import zel.game.players.Player
import zel.server.GameServices
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

private val log = LoggerFactory.getLogger(PetInventory::class.java)

private const val PETS_SQL = PetRows.SELECT + " WHERE pets.player_id = ? AND pets.room_id IS NULL"

class PetInventory(
    private val services: GameServices,
    private val player: Player,
) {
    private val byId = ConcurrentHashMap<Int, Pet>()

    init {
        val maker = checkNotNull(services.game.pets).maker
        try {
            services.database.queries
                .select(PETS_SQL) { int(player.profile.id) }
                .rows(maker::fromRow)
                .forEach { byId[it.id] = it }
        } catch (e: SqlFailure) {
            log.error("Could not read the pets of player {}", player.profile.id, e)
        }
    }

    val count: Int get() = byId.size

    val all: Collection<Pet> get() = byId.values

    operator fun get(petId: Int): Pet? = byId[petId]

    fun put(pet: Pet) {
        byId[pet.id] = pet
    }

    fun remove(pet: Pet) {
        byId.remove(pet.id)
    }

    fun dispose() =
        byId.values.filter { it.hasUnsavedChanges }.forEach { pet ->
            services.threading.run { pet.saveIfChanged() }
        }
}
