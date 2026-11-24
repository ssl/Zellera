package zel.wired.effect.user

import zel.game.items.CreditFurni
import zel.game.items.ItemInteraction
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions

object CloneSources {
    const val REFUSED = "wiredfurni.error.furni_can_not_be_cloned"

    fun allows(furni: RoomFurni): Boolean =
        furni.isBuildPass &&
            !furni.isWall &&
            Interactions.of(furni.definition).equals(ItemInteraction.DEFAULT, ignoreCase = true) &&
            CreditFurni.credits(furni.definition) <= 0
}
