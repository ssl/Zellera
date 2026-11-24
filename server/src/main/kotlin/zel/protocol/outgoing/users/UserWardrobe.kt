package zel.protocol.outgoing.users

import zel.game.players.inventory.Wardrobe
import zel.protocol.generated.WardrobeOut
import zel.protocol.generated.WardrobeOutCodec
import zel.protocol.outgoing.OutgoingMessage

class UserWardrobe(
    val wardrobe: Wardrobe,
) : OutgoingMessage<WardrobeOut>(WardrobeOutCodec) {
    override fun message() =
        WardrobeOut(
            0,
            wardrobe.outfits.values.map {
                WardrobeOut.List1Item(it.slot, it.figure, it.gender.name.uppercase())
            },
        )
}
