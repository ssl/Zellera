package zel.room.furni.behaviour.use

import zel.game.achievements.AchievementEvents
import zel.game.achievements.AchievementTally
import zel.game.catalog.RecyclerAchievements
import zel.game.items.GiftWrap
import zel.game.items.ItemInteraction
import zel.game.items.Items
import zel.game.items.OwnedFurni
import zel.game.items.PrizeBoxes
import zel.game.players.Player
import zel.protocol.generated.FurniListAddOrUpdateOut
import zel.protocol.generated.FurniListAddOrUpdateOutCodec
import zel.protocol.generated.FurniListInvalidateOut
import zel.protocol.generated.FurniListInvalidateOutCodec
import zel.protocol.generated.PresentOpenedOut
import zel.protocol.generated.PresentOpenedOutCodec
import zel.protocol.generated.StuffData
import zel.protocol.outgoing.inventory.AddRoomItem
import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.furni.FurniBehaviour
import zel.room.furni.RoomFurni
import zel.room.furni.Toggleable
import zel.room.furni.Usable
import zel.room.furni.behaviour.special.SpecialStuff
import zel.room.furni.behaviour.state.StateShow
import zel.room.furni.place.FurniPickup
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Placeable
import zel.room.furni.place.PlayerFurniInventory
import zel.room.furni.place.StuffOnWire
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class Gifts(
    private val items: () -> Items?,
    private val pickups: () -> FurniPickup?,
    private val inventory: PlayerFurniInventory,
    private val persist: (Runnable) -> Unit,
    private val post: (Int, (RoomScope) -> Unit) -> Unit,
    private val later: (RoomScope, RoomFurni, Long, (RoomScope, RoomFurni) -> Unit) -> Unit,
    private val now: () -> Int,
    private val plain: () -> FurniBehaviour?,
) : StuffOnWire,
    Usable,
    Toggleable,
    Placeable,
    RoomPart {
    /** The rows being opened in each loaded room; touched under that room's lock. */
    private val opening = ConcurrentHashMap<Room, MutableSet<Int>>()

    override fun load(room: Room) {
        opening[room] = HashSet()
    }

    override fun unload(scope: RoomScope) {
        opening.remove(scope.room)?.let(exploding::removeAll)
    }

    override fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        opening[scope.room]?.remove(furni.rowId)
        exploding -= furni.rowId
    }

    /** A recycler box shows and toggles like a plain furni; only the opening is the gifts'. */
    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        if (isRecyclerBox(furni)) (plain() as? Usable)?.onUse(scope, furni, playerId, param)
    }

    override fun toggle(
        scope: RoomScope,
        furni: RoomFurni,
        backward: Boolean,
    ): Boolean = isRecyclerBox(furni) && (plain() as? Toggleable)?.toggle(scope, furni, backward) == true

    override fun toggleRandom(
        scope: RoomScope,
        furni: RoomFurni,
        random: Random,
    ): Boolean = isRecyclerBox(furni) && (plain() as? Toggleable)?.toggleRandom(scope, furni, random) == true

    override fun stuff(furni: RoomFurni): StuffData {
        if (isRecyclerBox(furni)) return SpecialStuff.legacy(furni, FurniWire.floorState(furni))
        val wrap = GiftWrap.read(furni.state)
        return SpecialStuff.map(
            furni,
            mapOf(
                "EXTRA_PARAM" to "",
                "MESSAGE" to wrap.note,
                "PURCHASER_NAME" to wrap.shownSender,
                "PURCHASER_FIGURE" to wrap.shownLook,
                "PRODUCT_CODE" to "",
                "state" to if (furni.rowId in exploding) "1" else "0",
            ),
        )
    }

    private val exploding = ConcurrentHashMap.newKeySet<Int>()

    fun open(
        scope: RoomScope,
        player: Player,
        level: Int,
        itemId: Int,
    ) {
        val furni = scope.room.furni[itemId] ?: return
        if (level <= 0 || !furni.isSpendable || furni.ownerId != player.profile.id) return
        when {
            isRecyclerBox(furni) -> openBox(scope, player, furni)
            furni.behaviour === this -> openGift(scope, player, furni)
        }
    }

    private fun isRecyclerBox(furni: RoomFurni) = furni.definition.interaction.name == RECYCLER_BOX

    private fun openGift(
        scope: RoomScope,
        player: Player,
        furni: RoomFurni,
    ) {
        val busy = opening.getOrPut(scope.room) { HashSet() }
        if (!busy.add(furni.rowId)) return
        // A present whose params are set goes off before it opens.
        if (furni.definition.params.isNotBlank()) {
            exploding += furni.rowId
            StateShow.whole(scope, furni, furni.state)
            later(scope, furni, WRAP_MILLIS) { inner, box -> unwrap(inner, player, box) }
        } else {
            unwrap(scope, player, furni)
        }
    }

    private fun unwrap(
        scope: RoomScope,
        player: Player,
        furni: RoomFurni,
    ) {
        val roomId = scope.room.info.id
        val row = furni.rowId
        val contents = GiftWrap.read(furni.state).contentIds.toIntArray()
        persist(
            Runnable {
                val opened = items()?.prizeBoxes?.openGift(player, row, contents)
                if (opened != null) {
                    handOver(player, opened)
                    opened.firstOrNull()?.let { announce(player, it) }
                }
                post(roomId) { inner -> opened(inner, furni, opened != null) }
            },
        )
    }

    /** The contents are the player's once the write held, even if the box left the room. */
    private fun opened(
        scope: RoomScope,
        furni: RoomFurni,
        unwrapped: Boolean,
    ) {
        opening[scope.room]?.remove(furni.rowId)
        exploding -= furni.rowId
        if (unwrapped && scope.room.furni[furni.id] === furni) pickups()?.takeOut(scope, furni, picker = 0)
    }

    private fun openBox(
        scope: RoomScope,
        player: Player,
        furni: RoomFurni,
    ) {
        val busy = opening.getOrPut(scope.room) { HashSet() }
        if (!busy.add(furni.rowId)) return
        val roomId = scope.room.info.id
        val row = furni.rowId
        persist(
            Runnable {
                val reward = items()?.prizeBoxes?.openRecyclerBox(player, row, now())
                when (reward) {
                    is PrizeBoxes.BoxPrize.Furni -> {
                        handOver(player, listOf(reward.furni))
                        player.client?.send(FurniListInvalidateOutCodec, FurniListInvalidateOut)
                        announce(player, reward.furni)
                    }

                    // The present window shows furni only; the client's own chat style notification shows the bubble.
                    is PrizeBoxes.BoxPrize.ChatStyle -> {
                        player.services.chatStyles.tellGranted(player, reward.styleId)
                        AchievementEvents.identity().mirrorChatStyles(player)
                    }

                    null -> {}
                }
                if (reward != null) RecyclerAchievements.boxOpened(player, AchievementTally.running())
                post(roomId) { inner -> opened(inner, furni, reward != null) }
            },
        )
    }

    private fun handOver(
        player: Player,
        contents: List<OwnedFurni>,
    ) {
        player.inventory.furni.putAll(contents)
        val client = player.client ?: return
        client.send(FurniListInvalidateOutCodec, FurniListInvalidateOut)
        client.send(AddRoomItem(contents))
        val entries = contents.map(inventory::entry)
        client.send(FurniListAddOrUpdateOutCodec, FurniListAddOrUpdateOut(entries))
    }

    private fun announce(
        player: Player,
        item: OwnedFurni,
    ) {
        val base = item.definition
        val type = base.type.code.lowercase()
        val message = PresentOpenedOut(type, base.spriteId, base.name, item.id, type, false, "")
        player.client?.send(PresentOpenedOutCodec, message)
    }

    companion object {
        const val RECYCLER_BOX = ItemInteraction.RECYCLER_BOX
        private const val WRAP_MILLIS = 1000L
    }
}
