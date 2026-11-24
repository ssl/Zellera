package zel.game.games.snowball.arena

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import zel.game.games.snowball.arena.ArenaDefinition.Background
import zel.game.games.snowball.arena.ArenaDefinition.FuseObject
import zel.game.games.snowball.arena.ArenaDefinition.MachineRef
import zel.game.games.snowball.arena.ArenaDefinition.PileRef
import zel.game.games.snowball.arena.ArenaDefinition.Tile
import zel.game.games.snowball.arena.ArenaDefinition.TreeRef
import zel.game.games.snowball.sim.ArenaStage
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.random.RandomGenerator

/** The Snowball arenas of the content pack; a broken file is an error, never a silently smaller pool. */
class ArenaRepository internal constructor(
    arenas: Map<Int, ArenaDefinition>,
) {
    private val arenas: Map<Int, ArenaDefinition> = LinkedHashMap(arenas)

    fun get(fieldType: Int): ArenaDefinition? = arenas[fieldType]

    fun all(): Collection<ArenaDefinition> = arenas.values

    fun pick(
        allowed: List<Int>,
        random: RandomGenerator,
    ): ArenaDefinition? {
        val pool = allowed.mapNotNull { arenas[it] }.distinct()
        return if (pool.isEmpty()) null else pool[random.nextInt(pool.size)]
    }

    companion object {
        /** The arenas a game picks from unless `snowball.arenas` names others. */
        val FIELD_TYPES: List<Int> = listOf(8, 9, 11)

        internal const val MIN_SPAWNS_PER_TEAM = 5

        private val FILE_NAME = Regex("""(\d+)\.json""")

        /** Every `<fieldType>.json` in [folder]; a missing or empty folder gives no arenas. */
        fun load(folder: Path?): ArenaRepository {
            val arenas = sortedMapOf<Int, ArenaDefinition>()
            if (folder == null || !Files.isDirectory(folder)) return ArenaRepository(arenas)
            val files = Files.list(folder).use { list -> list.filter(Files::isRegularFile).toList() }
            for (file in files) {
                val match = FILE_NAME.matchEntire(file.fileName.toString()) ?: continue
                val fieldType = match.groupValues[1].toInt()
                try {
                    val arena = parse(Files.readString(file))
                    check(arena.fieldType == fieldType) { "Snowball arena $file holds fieldType ${arena.fieldType}" }
                    arenas[fieldType] = arena
                } catch (exception: IOException) {
                    throw IllegalStateException("Snowball arena $file cannot be read", exception)
                }
            }
            return ArenaRepository(arenas)
        }

        fun parse(json: String): ArenaDefinition {
            val root = JsonParser.parseString(json).asJsonObject
            val spawnObject = root.getAsJsonObject("spawns")
            val arena =
                ArenaDefinition(
                    fieldType = root.get("fieldType").asInt,
                    name = root.get("name").asString,
                    width = root.get("width").asInt,
                    height = root.get("height").asInt,
                    heightmap = root.getAsJsonArray("heightmap").map { it.asString },
                    fuse = root.getAsJsonArray("fuse").map { parseFuse(it.asJsonObject) },
                    trees =
                        root.getAsJsonArray("trees").map {
                            val o = it.asJsonObject
                            TreeRef(
                                o.get("fuse").asInt,
                                o.get("maxHits").asInt,
                                if (o.has("hits")) o.get("hits").asInt else 0,
                            )
                        },
                    piles =
                        root.getAsJsonArray("piles").map {
                            val o = it.asJsonObject
                            PileRef(o.get("fuse").asInt, o.get("max").asInt, o.get("count").asInt)
                        },
                    machines =
                        root.getAsJsonArray("machines").map {
                            val o = it.asJsonObject
                            MachineRef(o.get("fuse").asInt, o.get("max").asInt, o.get("count").asInt)
                        },
                    spawns = spawnObject.keySet().associate { it.toInt() to tiles(spawnObject.getAsJsonArray(it)) },
                )
            validate(arena)
            return arena
        }

        private fun parseFuse(o: JsonObject): FuseObject {
            val background =
                if (o.has("background")) {
                    val bg = o.getAsJsonObject("background")
                    Background(
                        bg.get("file").asString,
                        bg.get("offsetX").asInt,
                        bg.get("offsetY").asInt,
                        bg.get("offsetZ").asInt,
                    )
                } else {
                    null
                }
            return FuseObject(
                id = o.get("id").asInt,
                name = o.get("name").asString,
                x = o.get("x").asInt,
                y = o.get("y").asInt,
                xDim = o.get("xDim").asInt,
                yDim = o.get("yDim").asInt,
                dir = o.get("dir").asInt,
                height = o.get("height").asInt,
                altitude = o.get("altitude").asInt,
                canStandOn = o.get("canStandOn").asBoolean,
                state = if (o.has("state")) o.get("state").asString else "",
                background = background,
            )
        }

        private fun tiles(array: JsonArray): List<Tile> =
            array.map {
                val pair = it.asJsonArray
                Tile(pair.get(0).asInt, pair.get(1).asInt)
            }

        internal fun validate(arena: ArenaDefinition) {
            val where = "Snowball arena ${arena.fieldType} (${arena.name})"
            check(arena.heightmap.size == arena.height) {
                "$where: ${arena.heightmap.size} heightmap rows, height is ${arena.height}"
            }
            for (row in arena.heightmap) {
                check(row.length == arena.width) { "$where: a heightmap row is not ${arena.width} wide" }
            }

            val stage = ArenaStage(arena.width, arena.height, arena.heightmapString(), arena.fuse)
            val ids = HashSet<Int>()
            for (obj in arena.fuse) {
                check(ids.add(obj.id)) { "$where: fuse id ${obj.id} is used twice" }
                checkNotNull(stage.tile(obj.x, obj.y)) { "$where: ${obj.name} ${obj.id} has no floor" }
                check(ArenaDefinition.BACKGROUND_FURNI == obj.name == (obj.background != null)) {
                    "$where: ${obj.name} ${obj.id} and its background picture do not match"
                }
            }
            for (tree in arena.trees) {
                val fuse = requireFuse(arena, where, tree.fuse, "snst_tree")
                // a tree lowers its tile by its height on each construction, so extra tile height would break a resync
                val treeTile = stage.tile(fuse.x, fuse.y)
                check(treeTile == null || treeTile.height <= fuse.height) {
                    "$where: tree ${fuse.id} stands on ${treeTile?.height} units of furni, " +
                        "more than its own ${fuse.height}"
                }
            }
            arena.piles.forEach { requireFuse(arena, where, it.fuse, "snst_ballpile") }
            arena.machines.forEach { requireFuse(arena, where, it.fuse, "s_snowball_machine") }

            // a tile holds one game object: a human spawned on a tree, pile or machine would not be registered
            val taken = HashSet<Tile>()
            (arena.trees.map { it.fuse } + arena.piles.map { it.fuse } + arena.machines.map { it.fuse }).forEach {
                taken.add(gameObjectTile(arena, it))
            }
            for (team in 1..2) {
                val spawns = arena.spawns(team)
                check(spawns.size >= MIN_SPAWNS_PER_TEAM) { "$where: team $team has ${spawns.size} spawns" }
                for (spawn in spawns) {
                    val arenaTile = stage.tile(spawn.x, spawn.y)
                    check(arenaTile != null && arenaTile.walkable()) {
                        "$where: team $team spawn ${spawn.x},${spawn.y} is not walkable"
                    }
                    check(taken.add(spawn)) { "$where: spawn $spawn is used twice or holds a tree, pile or machine" }
                }
            }
        }

        private fun gameObjectTile(
            arena: ArenaDefinition,
            fuseId: Int,
        ): Tile {
            val obj = checkNotNull(arena.fuseObject(fuseId)) { "fuse $fuseId is missing" }
            return Tile(obj.x, obj.y)
        }

        private fun requireFuse(
            arena: ArenaDefinition,
            where: String,
            id: Int,
            namePrefix: String,
        ): FuseObject {
            val obj = arena.fuseObject(id)
            check(obj != null && obj.name.startsWith(namePrefix)) { "$where: fuse $id is not a $namePrefix" }
            return checkNotNull(obj)
        }
    }
}
