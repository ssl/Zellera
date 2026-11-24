package zel.wired.link

import zel.protocol.generated.WiredLinkOpenOut
import zel.protocol.generated.WiredLinkOpenOutCodec
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.furni.place.Placeable
import zel.room.tick.Audience
import zel.room.tick.Lane

class LinkFurni(
    private val dialog: LinkDialog,
    private val persist: (Runnable) -> Unit,
) : Usable,
    Placeable {
    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        if (furni.ownerId != playerId) return
        val open = WiredLinkOpenOut(furni.id)
        scope.out.send(Lane.Now, Audience.Player(playerId), WiredLinkOpenOutCodec, open)
    }

    override fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        val linkId = furni.id
        persist(Runnable { dialog.pickedUp(linkId) })
    }
}
