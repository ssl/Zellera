package zel.wired.chest

import zel.protocol.generated.CoinsChestContentsOut
import zel.protocol.generated.CoinsChestContentsOutCodec
import zel.protocol.generated.ItemsChestContentsChunkOutCodec
import zel.protocol.generated.OpenChestOut
import zel.protocol.generated.OpenChestOutCodec
import zel.protocol.generated.OutgoingCodec
import zel.protocol.generated.StuffData
import zel.room.Room
import zel.room.RoomScope
import zel.room.entry.DoorCheck
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.furni.place.Placeable
import zel.room.furni.place.StuffOnWire
import zel.room.tick.Audience
import zel.room.tick.Lane

class ChestFurni(
    private val chests: RoomChests,
    private val deals: ChestDeals,
    private val host: ChestHost,
    private val levelOf: (Room, Int) -> Int?,
) : Usable,
    Placeable,
    StuffOnWire {
    private enum class Approach { DOUBLE_CLICK, ASKED_FOR_CONTENTS }

    override fun stuff(furni: RoomFurni): StuffData =
        chests.chest(furni)?.let { ChestStuff.of(it, furni.state.ifEmpty { "0" }) }
            ?: StuffData(0, StuffData.Value.Legacy(furni.state))

    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        val chest = chests.chest(furni) ?: return
        lookInto(scope, furni, chest, playerId, Approach.DOUBLE_CLICK)
    }

    fun open(
        scope: RoomScope,
        playerId: Int,
        chestId: Int,
    ) {
        val (furni, chest) = chests.inRoom(scope.room, chestId) ?: return
        lookInto(scope, furni, chest, playerId, Approach.ASKED_FOR_CONTENTS)
    }

    fun close(
        scope: RoomScope,
        playerId: Int,
        chestId: Int,
    ) {
        val (furni, chest) = chests.inRoom(scope.room, chestId) ?: return
        chests.removeViewer(scope.room, chest.id, playerId)
        chests.showLid(scope, furni, chest)
    }

    private fun lookInto(
        scope: RoomScope,
        furni: RoomFurni,
        chest: Chest,
        playerId: Int,
        approach: Approach,
    ) {
        val mayOpen = chest.preferences.everyoneOpens || (levelOf(scope.room, playerId) ?: 0) >= DoorCheck.RIGHTS
        if (!mayOpen) return
        chests.addViewer(scope.room, chest.id, playerId)
        chests.showLid(scope, furni, chest)
        val clicked = approach == Approach.DOUBLE_CLICK
        when {
            chest.isCoins -> {
                if (clicked) send(scope, playerId, OpenChestOutCodec, OpenChestOut(chest.id))
                val coins = CoinsChestContentsOut(chest.id, chest.credits, clicked)
                send(scope, playerId, CoinsChestContentsOutCodec, coins)
            }

            !clicked || chests.wasShown(scope.room, playerId, chest.id) -> {
                chests.markShown(scope.room, playerId, chest.id)
                TableWire.chunks(chest).forEach { send(scope, playerId, ItemsChestContentsChunkOutCodec, it) }
            }

            else -> {
                send(scope, playerId, OpenChestOutCodec, OpenChestOut(chest.id))
            }
        }
    }

    private fun <T> send(
        scope: RoomScope,
        playerId: Int,
        codec: OutgoingCodec<T>,
        message: T,
    ) = scope.out.send(Lane.Now, Audience.Player(playerId), codec, message)

    override fun onPlaced(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        val chest = chests.chest(furni) ?: return
        if (!chest.placed) chest.setUp(host::starterTexts)
        chests.setLid(scope.room, furni, chest)
        chests.send(scope, furni)
        chests.save(scope.room.info.id, chest)
    }

    override fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        val chest = chests.chest(furni) ?: return
        deals.chestGone(scope, chest.id)
        furni.state = "0"
        chests.send(scope, furni)
        chests.saveLeaving(scope.room.info.id, chest)
        chests.forget(scope.room, chest.id)
    }
}
