package zel.game.pets

import zel.db.Sql
import java.sql.ResultSet
import java.util.EnumMap
import java.util.Random
import javax.sql.DataSource

class PetDefinitions private constructor(
    private val newKind: (ResultSet) -> PetKind,
    val kinds: Map<Int, PetKind>,
    val palettesByType: Map<Int, List<PetPalette>>,
    val babyBreeds: Map<Int, Map<BabyTier, List<Int>>>,
    val packagesByName: Map<String, PetPackage>,
    val boxes: List<PetBox>,
) {
    fun kindFrom(row: ResultSet): PetKind = newKind(row)

    companion object {
        /** `type_id` NULL: a line or furni that every pet type shares. */
        const val ALL_TYPES = -1

        private const val PALETTES_SQL =
            "SELECT type_id, breed, palette, sellable, rare, club_only FROM pet_palettes " +
                "ORDER BY type_id, breed, palette"

        fun load(
            dataSource: DataSource,
            random: () -> Random,
        ): PetDefinitions {
            val shared = SharedPetData()
            val newKind = { row: ResultSet ->
                PetKind(
                    row.getInt("id"),
                    row.getString("name"),
                    row.getInt("offspring_type_id").takeUnless { row.wasNull() } ?: PetKind.NO_OFFSPRING,
                    random,
                    shared,
                    PetBehaviour.stored(row.getString("behaviour")),
                    row.getString("product_name").orEmpty(),
                )
            }
            val kinds = dataSource.rows("SELECT * FROM pet_types ORDER BY id", newKind).associateBy { it.type }
            dataSource.each("SELECT type_id, mood, action FROM pet_type_actions") {
                kinds[it.getInt("type_id")]?.addPosture(it.getString("mood"), it.getString("action"))
            }
            giveCommands(dataSource, kinds)
            dataSource.each("SELECT type_id, situation, message FROM pet_vocals") {
                addLine(kinds, shared, typeOf(it), it.getString("situation"), it.getString("message"))
            }
            dataSource.each("SELECT type_id, definition_id FROM pet_furni") {
                val type = typeOf(it)
                if (type != ALL_TYPES) shared.furniRules.keepFor(it.getInt("definition_id"), type)
            }
            val palettes =
                dataSource
                    .rows(PALETTES_SQL) {
                        PetPalette(
                            it.getInt("type_id"),
                            it.getInt("breed"),
                            it.getInt("palette"),
                            it.getBoolean("sellable"),
                            it.getBoolean("rare"),
                            it.getBoolean("club_only"),
                        )
                    }.groupBy { it.petType }
            val babyBreeds = HashMap<Int, MutableMap<BabyTier, MutableList<Int>>>()
            dataSource.each("SELECT type_id, breed, tier FROM pet_baby_breeds ORDER BY type_id, breed") {
                val tier = BabyTier.stored(it.getString("tier")) ?: return@each
                val ofType = babyBreeds.getOrPut(it.getInt("type_id")) { EnumMap(BabyTier::class.java) }
                ofType.getOrPut(tier) { ArrayList() } += it.getInt("breed")
            }
            val packages =
                dataSource
                    .rows("SELECT * FROM pet_packages ORDER BY furni_code") {
                        PetPackage(
                            it.getString("furni_code"),
                            it.getInt("type_id"),
                            it.getInt("palette"),
                            it.getString("color"),
                            it.getString("nest_furni_code")?.takeIf(String::isNotBlank),
                        )
                    }.associateBy { it.itemName }
            return PetDefinitions(newKind, kinds, palettes, babyBreeds, packages, boxesOf(dataSource))
        }

        private fun boxesOf(dataSource: DataSource): List<PetBox> =
            dataSource.rows("SELECT furni_name, type_id, palette FROM pet_boxes") { row ->
                val palette = row.getInt("palette").takeUnless { row.wasNull() }
                PetBox(row.getString("furni_name"), row.getInt("type_id"), palette)
            }

        /** Files [message] under [situation] for one type or, with [ALL_TYPES], for all; false when it fits nowhere. */
        internal fun addLine(
            kinds: Map<Int, PetKind>,
            shared: SharedPetData,
            type: Int,
            situation: String,
            message: String?,
        ): Boolean {
            val known = PetSituation.named(situation) ?: return false
            if (message.isNullOrBlank()) return false
            val lines = if (type == ALL_TYPES) shared.lines else kinds[type]?.lines ?: return false
            lines.add(known, message)
            return true
        }

        /** Stored order first; commands without one follow by level. */
        fun sortForTraining(
            commands: MutableList<PetCommand>,
            order: Map<PetCommand, Int>,
        ) = commands.sortWith(compareBy({ order[it] ?: Int.MAX_VALUE }, { it.level }, { it.id }))

        private fun giveCommands(
            dataSource: DataSource,
            kinds: Map<Int, PetKind>,
        ) {
            val commands = dataSource.rows("SELECT * FROM pet_commands", PetCommand::stored).associateBy { it.id }
            val orders = HashMap<Int, MutableMap<PetCommand, Int>>()
            dataSource.each("SELECT type_id, command_id, required_level, sort_order FROM pet_type_commands") {
                val type = it.getInt("type_id")
                val kind = kinds[type] ?: return@each
                val common = commands[it.getInt("command_id")] ?: return@each
                val level = it.getInt("required_level")
                val command = if (it.wasNull()) common else common.openingAt(level)
                kind.commands += command
                val position = it.getInt("sort_order")
                if (!it.wasNull()) orders.getOrPut(type) { HashMap() }[command] = position
            }
            kinds.values.forEach { sortForTraining(it.commands, orders[it.type].orEmpty()) }
        }

        private fun typeOf(row: ResultSet): Int = row.getInt("type_id").takeUnless { row.wasNull() } ?: ALL_TYPES

        private fun <T> DataSource.rows(
            sql: String,
            read: (ResultSet) -> T,
        ): List<T> = Sql(this).select(sql).rows(read)

        private fun DataSource.each(
            sql: String,
            read: (ResultSet) -> Unit,
        ) = Sql(this).select(sql).each(read)
    }
}
