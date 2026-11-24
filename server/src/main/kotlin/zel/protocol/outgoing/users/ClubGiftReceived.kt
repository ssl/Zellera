package zel.protocol.outgoing.users

import zel.game.items.Item
import zel.game.items.ProductKind
import zel.protocol.generated.ClubGiftSelectedOut
import zel.protocol.generated.ClubGiftSelectedOutCodec
import zel.protocol.outgoing.OutgoingMessage

class ClubGiftReceived(
    val name: String,
    val items: Set<Item>,
) : OutgoingMessage<ClubGiftSelectedOut>(ClubGiftSelectedOutCodec) {
    override fun message() = ClubGiftSelectedOut(name, items.map(::entry))

    private fun entry(item: Item): ClubGiftSelectedOut.List1Item {
        val type = item.type
        val code = type.code.lowercase()
        if (type == ProductKind.BADGE) {
            return ClubGiftSelectedOut.List1Item(
                code,
                ClubGiftSelectedOut.List1Item.Branch1.Then(item.params),
            )
        }
        val itemName = item.name
        val detail =
            when {
                SINGLE_ROOM_ITEMS.any(itemName::contains) -> {
                    itemName.split("_").getOrElse(2) { "" }
                }

                type == ProductKind.BOT || itemName.equals("poster", ignoreCase = true) ||
                    itemName.startsWith("SONG ") -> {
                    item.params
                }

                else -> {
                    ""
                }
            }
        return ClubGiftSelectedOut.List1Item(
            code,
            ClubGiftSelectedOut.List1Item.Branch1.Else(
                item.spriteId,
                detail,
                1,
                false,
                ClubGiftSelectedOut.List1Item.Branch1.Else.Branch1.Else,
            ),
        )
    }

    private companion object {
        val SINGLE_ROOM_ITEMS = listOf("wallpaper_single", "floor_single", "landscape_single")
    }
}
