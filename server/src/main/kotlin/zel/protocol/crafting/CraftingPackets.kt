package zel.protocol.crafting

import zel.game.crafting.CraftingTable
import zel.game.crafting.Recipe
import zel.game.items.OwnedFurni
import zel.game.players.Player
import zel.protocol.generated.CraftInCodec
import zel.protocol.generated.CraftSecretInCodec
import zel.protocol.generated.CraftableProductsOut
import zel.protocol.generated.CraftableProductsOutCodec
import zel.protocol.generated.CraftingRecipeOut
import zel.protocol.generated.CraftingRecipeOutCodec
import zel.protocol.generated.CraftingRecipesAvailableOut
import zel.protocol.generated.CraftingRecipesAvailableOutCodec
import zel.protocol.generated.GetCraftableProductsInCodec
import zel.protocol.generated.GetCraftingRecipeInCodec
import zel.protocol.generated.GetCraftingRecipesAvailableInCodec
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.server.GameServices
import java.nio.ByteBuffer
import java.util.Collections
import java.util.IdentityHashMap

internal fun recipesLeft(
    matches: Map<Recipe, Boolean>,
    known: (Recipe) -> Boolean,
): Pair<Int, Boolean> {
    val beforeExact = matches.entries.takeWhile { (recipe, exact) -> known(recipe) || !exact }
    val found = beforeExact.size < matches.size
    return matches.size - beforeExact.count { known(it.key) } - (if (found) 1 else 0) to found
}

class CraftingPackets(
    private val services: GameServices,
) : PacketPackage {
    private val crafting get() = checkNotNull(services.game.crafting)
    private val delivery = CraftingDelivery(services)

    override val name = "crafting"
    override val routes =
        listOf(
            PacketRoute(GetCraftableProductsInCodec) { session, m -> sendCraftableProducts(session, m.itemId) },
            PacketRoute(GetCraftingRecipeInCodec) { session, m -> sendRecipe(session, m.recipeName) },
            PacketRoute(GetCraftingRecipesAvailableInCodec) { session, m ->
                sendRecipesAvailable(session, m.altarId, m.itemIds)
            },
            PacketRoute(CraftInCodec) { session, m -> craft(session, m.craftingTable, m.recipe) },
            PacketRoute(CraftSecretInCodec) { session, m ->
                craftSecret(session, m.altarId, ingredientIds(m.count, m.itemIds))
            },
        )

    private fun sendCraftableProducts(
        session: PacketSession,
        altarId: Int,
    ) {
        val player = session.client.player ?: return
        val altar = altarOf(player, altarId) ?: return
        session.send(
            CraftableProductsOutCodec,
            CraftableProductsOut(
                altar.recipesShownTo(player).map {
                    CraftableProductsOut.List1Item(it.name, it.product.name, it.product.name)
                },
                altar.materials.map { it.name },
            ),
        )
    }

    private fun sendRecipe(
        session: PacketSession,
        recipeName: String,
    ) {
        val player = session.client.player ?: return
        val recipe = crafting.recipe(recipeName) ?: return
        if (recipe.hidden && !player.stats.recipes.knows(recipe.id)) return
        if (!recipe.stock.available) {
            CatalogReplies.limitedSoldOut(session.client)
            return
        }
        session.send(
            CraftingRecipeOutCodec,
            CraftingRecipeOut(
                recipe.parts.map { (item, amount) -> CraftingRecipeOut.List1Item(amount, item.name) },
            ),
        )
    }

    private fun sendRecipesAvailable(
        session: PacketSession,
        altarId: Int,
        itemIds: List<Int>,
    ) {
        val player = session.client.player ?: return
        val altar = altarOf(player, altarId) ?: return
        if (itemIds.size > MAX_INGREDIENTS) return
        val given =
            itemIds
                .mapNotNull {
                    player.inventory.furni[it]?.definition
                }.groupingBy { it }
                .eachCount()
        val known = { recipe: Recipe -> player.stats.recipes.knows(recipe.id) }
        if (altar.recipeMadeOf(given)?.let(known) == true) return
        val (remaining, found) = recipesLeft(altar.recipesWithin(given), known)
        session.send(CraftingRecipesAvailableOutCodec, CraftingRecipesAvailableOut(remaining, found))
    }

    private fun altarOf(
        player: Player,
        furniId: Int,
    ): CraftingTable? =
        player.rooms
            .roomOf(player.profile.id)
            ?.let { player.rooms.furniIn(it, furniId) }
            ?.let { crafting.table(it.definition) }

    private fun craft(
        session: PacketSession,
        altarId: Int,
        recipeName: String,
    ) {
        val player = session.client.player ?: return
        val recipe = altarOf(player, altarId)?.recipeNamed(recipeName)
        when {
            recipe == null -> delivery.answerFailure(session)
            recipe.hidden && !player.stats.recipes.knows(recipe.id) -> delivery.answerFailure(session)
            !recipe.stock.available -> CatalogReplies.limitedSoldOut(session.client)
            hasIngredients(player, recipe) -> delivery.deliverNamed(session, player, recipe)
            else -> delivery.answerFailure(session)
        }
    }

    private fun hasIngredients(
        player: Player,
        recipe: Recipe,
    ): Boolean {
        val owned = player.inventory.furni.all
        return recipe.parts.all { (ingredient, amount) -> owned.count { it.definition === ingredient } >= amount }
    }

    private fun craftSecret(
        session: PacketSession,
        altarId: Int,
        itemIds: List<Int>,
    ) {
        val player = session.client.player ?: return
        val ingredients = secretIngredients(player, itemIds)
        val recipe =
            ingredients?.let {
                altarOf(player, altarId)?.recipeMadeOf(
                    it
                        .groupingBy { f ->
                            f.definition
                        }.eachCount(),
                )
            }
        when {
            ingredients == null || recipe == null -> delivery.answerFailure(session)
            !recipe.stock.available -> CatalogReplies.limitedSoldOut(session.client)
            else -> delivery.deliverSecret(session, player, recipe, ingredients)
        }
    }

    private fun ingredientIds(
        count: Int,
        bytes: ByteArray,
    ): List<Int> {
        if (count !in 1..MAX_INGREDIENTS || count * Int.SIZE_BYTES > bytes.size) return emptyList()
        val buffer = ByteBuffer.wrap(bytes)
        return List(count) { buffer.getInt() }
    }

    private fun secretIngredients(
        player: Player,
        itemIds: List<Int>,
    ): Set<OwnedFurni>? {
        if (itemIds.isEmpty() || itemIds.size > MAX_INGREDIENTS) return null
        val furni: MutableSet<OwnedFurni> = Collections.newSetFromMap(IdentityHashMap())
        val allFound =
            itemIds.all { id ->
                player.inventory.furni[id]?.let(furni::add) == true
            }
        return furni.takeIf { allFound }
    }

    private companion object {
        const val MAX_INGREDIENTS = 50
    }
}
