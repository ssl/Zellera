package zel.room.furni.behaviour.use

import zel.game.items.Item
import zel.game.players.Gender
import zel.game.players.Player
import zel.game.players.inventory.AvatarEffect
import zel.protocol.outgoing.generic.alerts.BubbleAlert
import zel.protocol.outgoing.generic.alerts.BubbleAlertMessage
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.furni.behaviour.state.StateShow

class FxBox(
    private val player: (Int) -> Player?,
    /** Takes the box out of the room and runs the hand-over once its row is gone. */
    private val spend: (RoomScope, RoomFurni, removeMillis: Int, onDeleted: () -> Unit) -> Boolean,
) : Usable {
    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        val owner = player(playerId) ?: return
        if (!furni.isSpendable || furni.ownerId != playerId || furni.state == USED) return
        val effectId = effectFor(furni.definition, owner.profile.gender)
        if (effectId <= 0) return
        val effects = owner.inventory.effects
        if (effects.ownsPermanent(effectId)) {
            owner.client?.send(BubbleAlertMessage(BubbleAlert.EFFECT_ALREADY_OWNED.key, HashMap()))
            return
        }
        StateShow.data(scope, furni, USED)
        spend(scope, furni, REMOVE_MILLIS) {
            if (!effects.ownsPermanent(effectId)) effects.grant(effectId, AvatarEffect.PERMANENT)
        }
    }

    private fun effectFor(
        definition: Item,
        gender: Gender,
    ): Int {
        val male = definition.effectMale
        val female = definition.effectFemale
        val effect = if (gender == Gender.F) female else male
        return if (effect > 0) effect else maxOf(male, female)
    }

    private companion object {
        const val USED = "1"
        const val REMOVE_MILLIS = 500
    }
}
