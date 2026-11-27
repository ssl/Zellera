package zel.room

import zel.core.rules.GameRules.PetRules
import zel.core.rules.rules
import zel.room.chat.HeadTurns
import zel.room.entry.EntryViews
import zel.room.life.RoomLife
import zel.room.protocol.BotPetPackets
import zel.room.tick.TickPart
import zel.room.unit.bot.BotSettings
import zel.room.unit.bot.BotStore
import zel.room.unit.bot.RoomBots
import zel.room.unit.pet.BotPetEntryViews
import zel.room.unit.pet.PetCare
import zel.room.unit.pet.PetCommands
import zel.room.unit.pet.PetLife
import zel.room.unit.pet.PetListener
import zel.room.unit.pet.PetStore
import zel.room.unit.pet.PetTuning
import zel.room.unit.pet.RoomPets
import zel.room.unit.walk.Walking
import zel.util.MILLIS_PER_SECOND

internal class BotsAndPets(
    services: RoomEngineServices,
    private val walking: Walking,
    private val roomLife: RoomLife,
    heads: HeadTurns,
    writes: RoomWrites,
) {
    private val units = services.units
    val bots = RoomBots(BotStore(services.dataSource), walking, units, heads::heard, writes)
    val pets =
        RoomPets(
            PetStore(services.dataSource),
            walking,
            units,
            services.persist,
            writes,
            { rules.roomMaxPets },
            petTuning(rules.pets),
        )
    private val life = PetLife(pets)
    private val care = PetCare(pets, services.persist)
    private val commands = PetCommands(pets, care)
    val tick: TickPart = TickPart.inOrder(bots.tick, commands.tick, life.tick)
    val listeners: List<RoomListener> = listOf(PetListener(life, commands, care))
    val parts: List<RoomPart> get() = listOf(bots, pets)

    fun entryViews(next: EntryViews): EntryViews = BotPetEntryViews(bots, pets, next)

    private fun botSettings() =
        BotSettings(bots, walking, units::filter) { units.config(MOTTO_LENGTH_KEY, MOTTO_LENGTH) }

    fun packets(registry: RoomRegistry): BotPetPackets =
        BotPetPackets(registry, bots, botSettings(), pets, care, roomLife)
}

private const val MOTTO_LENGTH_KEY = "player.motto.max.characters"
private const val MOTTO_LENGTH = 38

private fun petTuning(pets: PetRules) =
    PetTuning(
        needs =
            PetTuning.Needs(
                restEnergyDivisor = pets.restEnergyDivisor,
                thirstMillis = pets.thirstSeconds * MILLIS_PER_SECOND,
                walkingSadnessMillis = pets.walkingSadnessSeconds * MILLIS_PER_SECOND,
            ),
        moods =
            PetTuning.Moods(
                everyMillis = pets.moodEverySeconds * MILLIS_PER_SECOND,
                gestureMillis = pets.moodGestureSeconds * MILLIS_PER_SECOND,
                wearyEnergyDivisor = pets.wearyEnergyDivisor,
                thirstyFrom = pets.thirstyFrom,
                happyFromHappiness = pets.happyFromHappiness,
                sadUpToHappiness = pets.sadUpToHappiness,
            ),
        obedience =
            PetTuning.Obedience(
                restedPercent = pets.obeyRestedPercent,
                drainedPercent = pets.obeyDrainedPercent,
                drainedEnergyDivisor = pets.drainedEnergyDivisor,
                sulksBelowHappiness = pets.sulksBelowHappiness,
                gapMillis = pets.commandGapSeconds * MILLIS_PER_SECOND,
                repeatWindowMillis = pets.repeatWindowSeconds * MILLIS_PER_SECOND,
                repeatsAllowed = pets.repeatsAllowed,
                grumbleOneIn = pets.grumbleOneIn,
                stayMillis = pets.staySeconds * MILLIS_PER_SECOND,
            ),
        plants =
            PetTuning.Plants(
                rareParentFrom = pets.rarePlantParentFrom,
                oneSeedPercent = pets.onePlantSeedPercent,
            ),
    )
