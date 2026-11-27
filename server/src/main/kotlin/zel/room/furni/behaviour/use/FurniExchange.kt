package zel.room.furni.behaviour.use

import zel.game.items.CreditFurni
import zel.game.players.Player
import zel.protocol.generated.NotificationDialogOut
import zel.protocol.generated.NotificationDialogOutCodec
import zel.protocol.outgoing.users.FigureSetIds
import zel.room.RoomScope
import zel.room.entry.DoorCheck
import zel.room.furni.place.FurniPickup

class FurniExchange(
    private val pickups: FurniPickup,
    /** Deletes the row on the room's write lane and runs the given payout only once it is really gone. */
    private val delete: (roomId: Int, rowId: Int, onDeleted: () -> Unit, onFailed: () -> Unit) -> Unit,
    private val clothing: Clothing,
    private val inventoryChanged: (Player) -> Unit = { it.client?.send(FigureSetIds(it)) },
) {
    interface Clothing {
        /** Null when the furni's name is no clothing set. */
        fun owns(
            player: Player,
            classname: String,
        ): Boolean?

        fun grant(
            player: Player,
            classname: String,
        ): Boolean

        fun settle(
            player: Player,
            classname: String,
        )

        fun release(
            player: Player,
            classname: String,
        )
    }

    fun redeem(
        scope: RoomScope,
        player: Player,
        itemId: Int,
    ) {
        val furni = scope.room.furni[itemId] ?: return
        if (!furni.isSpendable || furni.ownerId != player.profile.id) return
        val credits = CreditFurni.credits(furni.definition)
        if (credits <= 0 || !pickups.takeOut(scope, furni)) return
        delete(scope.room.info.id, furni.rowId, { player.wallet.changeCredits(credits) }, {})
    }

    fun redeemClothing(
        scope: RoomScope,
        player: Player,
        level: Int,
        itemId: Int,
    ) {
        val furni = scope.room.furni[itemId] ?: return
        if (level < DoorCheck.RIGHTS || !furni.isSpendable || furni.ownerId != player.profile.id) return
        val name = furni.definition.name
        val owned = clothing.owns(player, name) ?: return
        if (owned) return tell(player, ALREADY)
        if (!pickups.takeOut(scope, furni)) return
        if (!clothing.grant(player, name)) return tell(player, ALREADY)
        delete(
            scope.room.info.id,
            furni.rowId,
            {
                clothing.settle(player, name)
                inventoryChanged(player)
                tell(player, DONE)
            },
            { clothing.release(player, name) },
        )
    }

    private fun tell(
        player: Player,
        type: String,
    ) {
        player.client?.send(NotificationDialogOutCodec, NotificationDialogOut(type, emptyList()))
    }

    private companion object {
        const val ALREADY = "figureset.already.redeemed"
        const val DONE = "figureset.redeemed.success"
    }
}
