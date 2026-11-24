package zel.game.items

import zel.game.players.Player
import java.sql.ResultSet

data class WelcomeGift(
    val image: String?,
    val furniName: String?,
    val roomCode: String?,
) {
    fun give(
        player: Player,
        items: Items,
    ) {
        val item = items.getItem(furniName ?: return) ?: return
        items.newFurni(player.profile.id, item, 0, 0, "")?.let(player.inventory.furni::deliver)
    }

    companion object {
        fun read(set: ResultSet): WelcomeGift {
            val value = set.getString("value")
            val isRoom = set.getString("kind").equals("room", ignoreCase = true)
            return WelcomeGift(set.getString("image"), value.takeUnless { isRoom }, value.takeIf { isRoom })
        }
    }
}
