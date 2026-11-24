package zel.game.items

import zel.game.pets.PlantRolls
import java.util.concurrent.ThreadLocalRandom

object MonsterPlantSeeds {
    fun rolledLevel(baseItem: Item?): String {
        val random = ThreadLocalRandom.current()
        val rarity = if (isRare(baseItem)) PlantRolls.freshRareRarity(random) else PlantRolls.freshRarity(random)
        return rarity.toString()
    }

    /** The params that tell the two seeds apart. */
    private const val COMMON_PARAMS = "1"
    private const val RARE_PARAMS = "8"

    fun isRare(baseItem: Item?): Boolean = baseItem?.params == RARE_PARAMS

    fun definition(
        items: Items,
        rare: Boolean,
    ): Item? = items.byBehaviour(ItemInteraction.MONSTER_PLANT_SEED, if (rare) RARE_PARAMS else COMMON_PARAMS)
}
