package zel.wired.chest

import zel.protocol.generated.ChestPreferencesUpdateSuccessOut
import zel.protocol.generated.ChestPreferencesUpdateSuccessOutCodec
import zel.protocol.generated.ObjectDataUpdateOut
import zel.protocol.generated.ObjectDataUpdateOutCodec
import zel.protocol.generated.SetChestNotificationSettingsIn
import zel.protocol.generated.SetChestOptionsIn
import zel.protocol.generated.SetChestPreferencesIn
import zel.protocol.generated.UpgradeChestResultOut
import zel.protocol.generated.UpgradeChestResultOutCodec
import zel.room.RoomScope
import zel.room.entry.DoorCheck
import zel.room.furni.RoomFurni
import zel.room.furni.place.FurniWire
import zel.room.tick.Audience
import zel.room.tick.Lane

class ChestSettings(
    private val chests: RoomChests,
    private val host: ChestHost,
) {
    /** Owner only; room rights never switch on wired or open someone's chest. */
    fun preferences(
        scope: RoomScope,
        playerId: Int,
        m: SetChestPreferencesIn,
    ) {
        val (furni, chest) = chests.inRoom(scope.room, m.itemId) ?: return
        if (furni.ownerId != playerId) return
        chest.preferences = chest.preferences.after(m) { host.filtered(playerId, it) }
        chests.changed(scope, furni, chest)
        success(scope, playerId, chest, notifications = false)
    }

    fun options(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        m: SetChestOptionsIn,
    ) {
        val (furni, chest) = chests.inRoom(scope.room, m.itemId) ?: return
        if (furni.ownerId == playerId) {
            chest.locked = m.locked
            chest.autoLock = m.autoLock
            chest.capacity = m.capacity.coerceIn(0, chest.capacityMax)
            chests.changed(scope, furni, chest)
        } else if (level >= DoorCheck.RIGHTS && m.locked && !chest.locked) {
            chest.locked = true
            chests.changed(scope, furni, chest)
        }
    }

    fun notifications(
        scope: RoomScope,
        playerId: Int,
        m: SetChestNotificationSettingsIn,
    ) {
        val (furni, chest) = chests.inRoom(scope.room, m.itemId) ?: return
        if (furni.ownerId != playerId) return
        chest.notifications = ChestNotifications.of(m)
        chests.changed(scope, furni, chest)
        success(scope, playerId, chest, notifications = true)
    }

    fun lockAll(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        lock: Boolean,
        all: Boolean,
    ) {
        val everyChest = lock && all && level >= DoorCheck.OWNER
        chests
            .all(scope.room)
            .filter { (furni, chest) -> (everyChest || furni.ownerId == playerId) && chest.locked != lock }
            .forEach { (furni, chest) ->
                chest.locked = lock
                chests.changed(scope, furni, chest)
            }
    }

    fun upgrade(
        scope: RoomScope,
        playerId: Int,
        chestId: Int,
        quantity: Int,
    ) {
        val (furni, chest) = chests.inRoom(scope.room, chestId) ?: return
        val refused =
            when {
                furni.ownerId != playerId -> RESULT_NOT_OWNER
                chest.type.starter -> RESULT_STARTER
                quantity <= 0 -> RESULT_NO_QUANTITY
                quantity > chest.type.maxSteps - chest.level -> RESULT_PAST_MAX
                else -> null
            }
        if (refused != null) return result(scope, playerId, chestId, refused)
        // a quantity within the steps left keeps the cost far inside an Int
        val cost = quantity * UPGRADE_COST
        val paid =
            host.withWallet(playerId) {
                val affordable = host.credits(playerId) >= cost && host.premium(playerId) >= cost
                if (affordable) {
                    val ceilingFollows = chest.capacity >= chest.capacityMax || chest.capacity < 0
                    host.takeCredits(playerId, cost)
                    chest.capacityMax += chest.type.step * quantity
                    sendData(scope, furni)
                    host.takePremium(playerId, cost)
                    if (ceilingFollows) chest.capacity = chest.capacityMax
                }
                affordable
            }
        if (!paid) return result(scope, playerId, chestId, RESULT_NO_FUNDS)
        result(scope, playerId, chestId, RESULT_OK)
        chests.changed(scope, furni, chest)
    }

    private fun sendData(
        scope: RoomScope,
        furni: RoomFurni,
    ) = scope.out.send(
        Lane.Now,
        Audience.Room,
        ObjectDataUpdateOutCodec,
        ObjectDataUpdateOut(furni.id.toString(), FurniWire.stuff(furni)),
    )

    private fun result(
        scope: RoomScope,
        playerId: Int,
        chestId: Int,
        code: Int,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(playerId),
        UpgradeChestResultOutCodec,
        UpgradeChestResultOut(chestId, code),
    )

    private fun success(
        scope: RoomScope,
        playerId: Int,
        chest: Chest,
        notifications: Boolean,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(playerId),
        ChestPreferencesUpdateSuccessOutCodec,
        ChestPreferencesUpdateSuccessOut(chest.id, notifications),
    )

    private companion object {
        const val UPGRADE_COST = 10
        const val RESULT_OK = 0
        const val RESULT_PAST_MAX = 2
        const val RESULT_NO_FUNDS = 3
        const val RESULT_NOT_OWNER = 7
        const val RESULT_NO_QUANTITY = 9
        const val RESULT_STARTER = 10
    }
}
