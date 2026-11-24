package zel.protocol.crafting

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.game.achievements.GameEvents
import zel.game.achievements.count
import zel.game.crafting.Recipe
import zel.game.crafting.RecipeStockStore
import zel.game.items.OwnedFurni
import zel.game.items.exchangeInventoryFurni
import zel.game.players.Player
import zel.protocol.generated.CraftingResultOut
import zel.protocol.generated.CraftingResultOutCodec
import zel.protocol.generated.FurniListInvalidateOut
import zel.protocol.generated.FurniListInvalidateOutCodec
import zel.protocol.generated.FurniListRemoveOut
import zel.protocol.generated.FurniListRemoveOutCodec
import zel.protocol.outgoing.inventory.AddRoomItem
import zel.protocol.typed.PacketSession
import zel.server.GameServices

internal class CraftingDelivery(
    private val services: GameServices,
) {
    fun deliverNamed(
        session: PacketSession,
        player: Player,
        recipe: Recipe,
    ) {
        val taken = takeIngredients(session, player, recipe) ?: return answerFailure(session)
        val reward = craft(player, recipe, taken)
        if (reward == null) {
            putBack(session, player, taken)
            answerFailure(session)
            return
        }
        finish(session, player, recipe, reward, taken)
    }

    /** Any of the placed furni gone refuses the whole craft. */
    fun deliverSecret(
        session: PacketSession,
        player: Player,
        recipe: Recipe,
        ingredients: Set<OwnedFurni>,
    ) {
        val stash = player.inventory.furni
        if (!stash.takeAll(ingredients)) {
            answerFailure(session)
            return
        }
        val reward = craft(player, recipe, ingredients)
        if (reward == null) {
            stash.putBack(ingredients)
            answerFailure(session)
            return
        }
        finish(session, player, recipe, reward, ingredients)
    }

    /** The answer first, then the inventory it explains, then what the craft counts for. */
    private fun finish(
        session: PacketSession,
        player: Player,
        recipe: Recipe,
        reward: OwnedFurni,
        used: Collection<OwnedFurni>,
    ) {
        answerSuccess(session, recipe)
        hand(session, player, reward)
        consume(session, used)
        if (recipe.hidden) player.stats.recipes.learn(recipe.id)
        val achievements = checkNotNull(services.game.achievements)
        // a recipe row may name its own achievement
        if (recipe.achievement.isNotEmpty()) achievements.named(recipe.achievement)?.progress(player)
        val crafted = if (recipe.hidden) GameEvents.CRAFTING_SECRET_CRAFTED else GameEvents.CRAFTING_CRAFTED
        achievements.count(player, crafted)
    }

    private fun consume(
        session: PacketSession,
        taken: Collection<OwnedFurni>,
    ) {
        taken.forEach { session.client.send(FurniListRemoveOutCodec, FurniListRemoveOut(it.stripId)) }
        session.client.send(FurniListInvalidateOutCodec, FurniListInvalidateOut)
    }

    fun answerFailure(session: PacketSession) =
        session.send(
            CraftingResultOutCodec,
            CraftingResultOut(false, CraftingResultOut.Branch1.Else),
        )

    private fun answerSuccess(
        session: PacketSession,
        recipe: Recipe,
    ) = session.send(
        CraftingResultOutCodec,
        CraftingResultOut(true, CraftingResultOut.Branch1.Then(recipe.name, recipe.product.name, recipe.product.name)),
    )

    private fun takeIngredients(
        session: PacketSession,
        player: Player,
        recipe: Recipe,
    ): List<OwnedFurni>? {
        val needed = recipe.parts.flatMap { (ingredient, amount) -> List(amount) { ingredient } }
        val taken = ArrayList<OwnedFurni>(needed.size)
        for (ingredient in needed) {
            val furni = player.inventory.furni.takeFirstOf(ingredient)
            if (furni == null) {
                putBack(session, player, taken)
                return null
            }
            taken += furni
        }
        return taken
    }

    @Suppress("TooGenericExceptionCaught") // any failure means nothing crafted
    private fun craft(
        player: Player,
        recipe: Recipe,
        ingredients: Collection<OwnedFurni>,
    ): OwnedFurni? {
        if (!recipe.stock.take()) return null
        val items = checkNotNull(services.game.items)
        val ownerId = player.profile.id
        val reward =
            try {
                services.database.dataSource.exchangeInventoryFurni(ownerId, ingredients) { connection ->
                    if (RecipeStockStore.take(connection, recipe)) {
                        items.newFurni(connection, ownerId, recipe.product, 0, 0, "")
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                log.warn("Craft of recipe {} by player {} rolled back", recipe.id, ownerId, e)
                null
            }
        if (reward == null) recipe.stock.giveBack()
        return reward
    }

    private fun hand(
        session: PacketSession,
        player: Player,
        reward: OwnedFurni,
    ) {
        player.inventory.furni.put(reward)
        session.client.send(AddRoomItem(reward))
    }

    private fun putBack(
        session: PacketSession,
        player: Player,
        furni: List<OwnedFurni>,
    ) {
        if (furni.isEmpty()) return
        furni.forEach { hand(session, player, it) }
        session.client.send(FurniListInvalidateOutCodec, FurniListInvalidateOut)
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(CraftingDelivery::class.java)
    }
}
