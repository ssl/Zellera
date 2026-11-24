package zel.game.pets

import zel.db.intOrNone
import zel.db.write
import zel.protocol.generated.PacketWriter
import java.sql.Connection
import java.sql.ResultSet

class MonsterPlant :
    Pet,
    PetLook {
    data class Feature(
        val slot: Int,
        val style: Int,
        val tint: Int,
    )

    /** The body (the style of slot 1). */
    val body: Int

    /** The colour (the tint of slot 1). */
    val hue: Int

    /** The pot's tint (slot 0). */
    val baseTint: Int

    private val face: List<Feature>

    private var deathAt: Int
    private var markedDead: Boolean

    /** The stage it last showed in a loaded room; a dead plant keeps showing it. */
    private var shownStage: Int
    private var storedBreedable: Boolean

    var breedable: Boolean
        get() = storedBreedable
        set(value) {
            storedBreedable = value
            markChanged()
        }

    var allowsBreeding: Boolean

    /** A plant stored without a name goes by its colour and body. */
    override var name: String
        get() = super.name.ifBlank { nameOf(body, hue) }
        set(value) {
            super.name = value
        }

    constructor(pets: PetsModule, set: ResultSet) : super(pets, PetRow.read(set)) {
        body = set.getInt("kind")
        hue = set.getInt("plant_color")
        baseTint = set.getInt("base_palette")
        face =
            faceOf(
                set.getInt("nose"),
                set.getInt("nose_color"),
                set.getInt("eyes"),
                set.getInt("eyes_color"),
                set.getInt("mouth"),
                set.getInt("mouth_color"),
            )
        deathAt = set.intOrNone("dies_at", created + SPROUT_LIFESPAN_SECONDS)
        markedDead = set.getBoolean("is_dead")
        shownStage = set.intOrNone("shown_stage", clockStage())
        storedBreedable = set.getBoolean("breedable")
        allowsBreeding = set.getBoolean("allow_breeding")
    }

    @Suppress("LongParameterList") // the seed decides every feature
    constructor(
        pets: PetsModule,
        ownerId: Int,
        body: Int,
        colour: Int,
        nose: Int,
        noseTint: Int,
        eyes: Int,
        eyesTint: Int,
        mouth: Int,
        mouthTint: Int,
    ) : super(pets, newRow(pets, PetKind.MONSTERPLANT_TYPE, 0, WHITE, nameOf(body, colour), ownerId)) {
        this.body = body
        hue = colour
        baseTint = pets.services.random().nextInt(POT_TINTS)
        face = faceOf(nose, noseTint, eyes, eyesTint, mouth, mouthTint)
        deathAt = created + SPROUT_LIFESPAN_SECONDS
        markedDead = false
        shownStage = 1
        storedBreedable = true
        allowsBreeding = false
    }

    val rarity: Int get() = rarityFor(body, hue)

    val isDead: Boolean get() = markedDead || now() >= deathAt

    val stage: Int get() = if (isDead) shownStage else clockStage()

    val isGrown: Boolean get() = stage == GROWN_STAGE

    val isBreedable: Boolean get() = isGrown && storedBreedable && !isDead

    override val maxEnergy: Int get() = MAX_ENERGY

    /** Remaining well-being on the energy scale; it can't be set. */
    override var energy: Int
        get() = (secondsToLive().toLong() * MAX_ENERGY / lifespanSeconds()).toInt()
        set(_) = Unit

    /** Shown as nutrition, which a plant always has full. */
    override var happiness: Int
        get() = FULL_NUTRITION
        set(_) = Unit

    /** Growth points; they follow from the time grown. */
    override var experience: Int
        get() = growth()
        set(_) = Unit

    override val breedId: Int get() = 0

    override val look: String get() = "${PetKind.MONSTERPLANT_TYPE} 0 $WHITE ${figureText()}"

    fun now(): Int = services.now()

    private fun figure(): List<Feature> {
        val sprout = listOf(Feature(POT, -1, baseTint), Feature(BODY, body, hue))
        return if (isGrown) sprout + face else sprout
    }

    /** The features as figure text: count, then slot, style and tint each. */
    fun figureText(): String =
        figure().let { features ->
            (listOf(features.size) + features.flatMap { listOf(it.slot, it.style, it.tint) }).joinToString(" ")
        }

    override fun writeParts(body: PacketWriter) {
        val features = figure()
        body.int(features.size)
        features.forEach { body.int(it.slot).int(it.style).int(it.tint) }
    }

    override fun writeTo(body: PacketWriter) {
        body
            .int(id)
            .string(name)
            .int(PetKind.MONSTERPLANT_TYPE)
            .int(race)
            .string(WHITE)
            .int(breedId)
        writeParts(body)
        body.int(stage)
    }

    fun secondsToLive(): Int = if (markedDead) 0 else maxOf(0, deathAt - now())

    /** Seconds alive so far; a dead plant's clock stopped at its death. */
    private fun ageSeconds(): Int = maxOf(0, minOf(now(), deathAt) - created)

    private fun growth(): Int = minOf(POINTS_TO_GROW, ageSeconds() / SECONDS_PER_POINT)

    private fun clockStage(): Int = minOf(GROWN_STAGE, growth() / POINTS_PER_STAGE + 1)

    /** A living plant in a loaded room shows the clock's stage; true when that is a stage it had not shown yet. */
    fun showClockStage(): Boolean {
        if (isDead || clockStage() == shownStage) return false
        shownStage = clockStage()
        markChanged()
        return true
    }

    fun stageGoal(): Int = minOf(POINTS_TO_GROW, stage * POINTS_PER_STAGE)

    fun lifespanSeconds(): Int = if (isGrown) GROWN_LIFESPAN_SECONDS else SPROUT_LIFESPAN_SECONDS

    fun secondsToGrow(): Int = maxOf(0, GROW_TIME_SECONDS - ageSeconds())

    /** Skips what is left of the growing time. */
    fun finishGrowing() {
        created -= secondsToGrow()
        markChanged()
    }

    fun dieAt(at: Int) {
        deathAt = at
        markChanged()
    }

    fun restoreLife() {
        val now = now()
        if (now > deathAt) created += now - deathAt
        markedDead = false
        deathAt = now + lifespanSeconds()
        markChanged()
    }

    override fun storeExtras(connection: Connection) {
        connection.write(UPDATE_SQL) {
            int(body)
            int(hue)
            face.forEach {
                int(it.style)
                int(it.tint)
            }
            int(baseTint)
            int(deathAt)
            boolean(isDead)
            int(shownStage)
            boolean(storedBreedable)
            boolean(allowsBreeding)
            int(id)
        }
    }

    data class PlantPart(
        val name: String,
        val rarity: Int,
    )

    companion object {
        private const val POT_TINTS = 11
        private const val POT = 0
        private const val BODY = 1
        private const val NOSE = 2
        private const val EYES = 3
        private const val MOUTH = 4
        private const val UPDATE_SQL =
            "UPDATE monster_plants SET kind = ?, color = ?, nose = ?, nose_color = ?, eyes = ?, eyes_color = ?, " +
                "mouth = ?, mouth_color = ?, base_palette = ?, dies_at = ?, is_dead = ?, shown_stage = ?, " +
                "breedable = ?, allow_breeding = ? WHERE pet_id = ?"

        val BODIES: Map<Int, PlantPart> =
            linkedMapOf(
                1 to PlantPart("Blungon", 0),
                2 to PlantPart("Wailzor", 1),
                3 to PlantPart("Stumpy", 1),
                4 to PlantPart("Sunspike", 2),
                5 to PlantPart("Squarg", 0),
                6 to PlantPart("Shroomer", 3),
                7 to PlantPart("Zuchinu", 3),
                8 to PlantPart("Abysswirl", 5),
                9 to PlantPart("Weggytum", 2),
                10 to PlantPart("Wystique", 4),
                11 to PlantPart("Hairbullis", 4),
                12 to PlantPart("Snozzle", 6),
            )

        val COLOURS: Map<Int, PlantPart> =
            linkedMapOf(
                0 to PlantPart("Aenueus", 0),
                1 to PlantPart("Griseus", 1),
                2 to PlantPart("Phoenicus", 2),
                3 to PlantPart("Viridulus", 1),
                4 to PlantPart("Cyaneus", 5),
                5 to PlantPart("Incarnatus", 2),
                6 to PlantPart("Azureus", 4),
                7 to PlantPart("Atamasc", 4),
                8 to PlantPart("Amethyst", 3),
                9 to PlantPart("Fulvus", 0),
                10 to PlantPart("Cinereus", 3),
            )

        val TOP_RARITY: Int = BODIES.values.maxOf { it.rarity } + COLOURS.values.maxOf { it.rarity }

        const val GROWN_STAGE = 7
        private const val POINTS_PER_STAGE = 20
        private const val POINTS_TO_GROW = (GROWN_STAGE - 1) * POINTS_PER_STAGE
        private const val SECONDS_PER_POINT = 1440
        const val GROW_TIME_SECONDS = POINTS_TO_GROW * SECONDS_PER_POINT
        private const val HOUR = 3600
        const val SPROUT_LIFESPAN_SECONDS = 36 * HOUR
        private const val GROWN_LIFESPAN_SECONDS = 72 * HOUR
        private const val MAX_ENERGY = 120
        private const val FULL_NUTRITION = 100

        fun rarityFor(
            body: Int,
            colour: Int,
        ): Int = (BODIES[body]?.rarity ?: 0) + (COLOURS[colour]?.rarity ?: 0)

        @Suppress("LongParameterList") // one part and one tint per face feature
        private fun faceOf(
            nose: Int,
            noseTint: Int,
            eyes: Int,
            eyesTint: Int,
            mouth: Int,
            mouthTint: Int,
        ): List<Feature> =
            listOf(Feature(NOSE, nose, noseTint), Feature(EYES, eyes, eyesTint), Feature(MOUTH, mouth, mouthTint))

        private fun nameOf(
            body: Int,
            colour: Int,
        ): String = "${COLOURS[colour]?.name.orEmpty()} ${BODIES[body]?.name.orEmpty()}".trim()
    }
}
