package zel.game.pets

import org.slf4j.LoggerFactory
import zel.db.Params
import zel.db.Sql
import zel.db.orFallback
import zel.game.achievements.Achievement
import zel.game.achievements.GameEvents

/** Turns pet happenings into game events; the pet type id is the detail where the content pack may tell kinds apart. */
object PetAchievements {
    private val log = LoggerFactory.getLogger(PetAchievements::class.java)

    /** [event] with [pet]'s type id as detail; a pet without a known type counts on the bare event. */
    internal fun ofPet(
        event: String,
        pet: Pet?,
    ): String = pet?.kind?.type?.let { GameEvents.of(event, it) } ?: event

    /** Every "code" is an event: it stands for each achievement the content pack hangs on it. */
    internal interface Ledger {
        fun standing(
            playerId: Int,
            code: String,
        ): Int

        fun credit(
            playerId: Int,
            code: String,
            amount: Int,
        )

        fun ownedBreeds(
            playerId: Int,
            petType: Int,
        ): Int

        /** Whether any achievement counts on [code]; spares the lookups behind a lift nobody counts. */
        fun counts(code: String): Boolean = true

        /** Raises the progress on [code] to [target]; progress never goes down. */
        fun liftTo(
            playerId: Int,
            code: String,
            target: Int,
        ) {
            if (target <= 0) return
            val amount = shortfall(standing(playerId, code), target)
            if (amount > 0) credit(playerId, code, amount)
        }
    }

    fun credit(
        services: PetServices,
        event: PetEvent,
    ) = credit(event, LiveLedger(services))

    internal fun credit(
        event: PetEvent,
        ledger: Ledger,
    ) {
        when (event) {
            is PetEvent.LevelGained -> ledger.credit(event.pet.userId, ofPet(GameEvents.PET_LEVEL_UP, event.pet), 1)
            is PetEvent.Scratched -> scratched(event, ledger)
            is PetEvent.Fed -> fed(event, ledger)
            is PetEvent.HorseJumped -> horseJumped(event, ledger)
            is PetEvent.Mounted -> mounted(event, ledger)
            is PetEvent.PlantsInRoom -> plantsInRoom(event, ledger)
            is PetEvent.SeedsHandedOut -> seedsHandedOut(event, ledger)
            is PetEvent.PetAcquired -> petAcquired(event, ledger)
        }
    }

    private fun scratched(
        event: PetEvent.Scratched,
        ledger: Ledger,
    ) {
        if (event.byPlayerId > 0) ledger.credit(event.byPlayerId, ofPet(GameEvents.PET_SCRATCHED, event.pet), 1)
    }

    private fun fed(
        event: PetEvent.Fed,
        ledger: Ledger,
    ) {
        if (event.points > 0) ledger.credit(event.pet.userId, ofPet(GameEvents.PET_FED, event.pet), event.points)
    }

    private fun horseJumped(
        event: PetEvent.HorseJumped,
        ledger: Ledger,
    ) {
        if (isGuest(event.riderId, event.roomOwnerId)) {
            ledger.credit(event.roomOwnerId, GameEvents.HORSE_JUMP_BY_GUEST, 1)
        }
        ledger.liftTo(event.riderId, GameEvents.HORSE_JUMP_STREAK, event.streak)
    }

    /** Only a horse is rented, not any rideable pet. */
    private fun mounted(
        event: PetEvent.Mounted,
        ledger: Ledger,
    ) {
        val horseOwnerId = (event.mount as? HorsePet)?.userId ?: return
        if (isGuest(event.riderId, horseOwnerId)) ledger.credit(horseOwnerId, GameEvents.HORSE_RIDDEN_BY_GUEST, 1)
    }

    private fun seedsHandedOut(
        event: PetEvent.SeedsHandedOut,
        ledger: Ledger,
    ) = watchersWithoutSeed(event).forEach { ledger.credit(it, GameEvents.PLANT_NO_SEED, 1) }

    internal fun isGuest(
        playerId: Int,
        ownerId: Int,
    ): Boolean = playerId > 0 && ownerId > 0 && playerId != ownerId

    /** What a "have N" achievement still needs to reach [target]; its progress never goes down. */
    internal fun shortfall(
        have: Int,
        target: Int,
    ): Int = maxOf(0, target - maxOf(0, have))

    internal fun distinctBodies(pets: Collection<Pet>): Int = plants(pets).distinctBy { it.body }.size

    internal fun distinctHues(pets: Collection<Pet>): Int = plants(pets).distinctBy { it.hue }.size

    private fun plants(pets: Collection<Pet>) = pets.filterIsInstance<MonsterPlant>()

    internal fun watchersWithoutSeed(event: PetEvent.SeedsHandedOut): List<Int> =
        event.watchers.distinct().filter { it > 0 && it != event.firstSeedTo && it != event.secondSeedTo }

    private fun plantsInRoom(
        event: PetEvent.PlantsInRoom,
        ledger: Ledger,
    ) {
        if (event.roomOwnerId <= 0) return
        ledger.liftTo(event.roomOwnerId, GameEvents.PLANT_SHAPES_IN_ROOM, distinctBodies(event.pets))
        ledger.liftTo(event.roomOwnerId, GameEvents.PLANT_COLOURS_IN_ROOM, distinctHues(event.pets))
    }

    private fun petAcquired(
        event: PetEvent.PetAcquired,
        ledger: Ledger,
    ) {
        val petType = event.pet.kind?.type ?: return
        if (event.ownerId <= 0) return
        val code = GameEvents.of(GameEvents.PET_BABY, petType)
        if (ledger.counts(code)) ledger.liftTo(event.ownerId, code, ledger.ownedBreeds(event.ownerId, petType))
    }

    /** An online player's progress is in memory; an offline player's is the stored row. */
    private class LiveLedger(
        private val services: PetServices,
    ) : Ledger {
        override fun counts(code: String): Boolean = live(code).isNotEmpty()

        /** The lowest standing among the event's achievements; [liftTo] raises each on its own. */
        override fun standing(
            playerId: Int,
            code: String,
        ): Int = live(code).minOfOrNull { standingOf(playerId, it) } ?: 0

        override fun credit(
            playerId: Int,
            code: String,
            amount: Int,
        ) = live(code).forEach { it.progressFor(playerId, amount) }

        override fun liftTo(
            playerId: Int,
            code: String,
            target: Int,
        ) {
            if (target <= 0) return
            live(code).forEach { definition ->
                val amount = shortfall(standingOf(playerId, definition), target)
                if (amount > 0) definition.progressFor(playerId, amount)
            }
        }

        private fun live(code: String): List<Achievement> =
            services.achievements().forEvent(code).filter { it.state.toInt() == 1 }

        private fun standingOf(
            playerId: Int,
            definition: Achievement,
        ): Int {
            val online = services.players().find(playerId)?.takeIf { it.isOnline }
            if (online != null) return maxOf(0, online.stats.progressOf(definition))
            return count(STANDING_SQL) {
                int(playerId)
                int(definition.id)
            }
        }

        override fun ownedBreeds(
            playerId: Int,
            petType: Int,
        ): Int =
            count(OWNED_BREEDS_SQL) {
                int(playerId)
                int(petType)
            }

        private fun count(
            sql: String,
            bind: Params.() -> Unit,
        ): Int =
            log.orFallback("reading a pet achievement count", 0) {
                Sql(services.dataSource).select(sql, bind).first { maxOf(0, it.getInt(1)) } ?: 0
            }

        private companion object {
            const val OWNED_BREEDS_SQL = "SELECT COUNT(DISTINCT race) FROM pets WHERE player_id = ? AND type_id = ?"
            const val STANDING_SQL =
                "SELECT progress FROM player_achievements WHERE player_id = ? AND achievement_id = ?"
        }
    }
}
