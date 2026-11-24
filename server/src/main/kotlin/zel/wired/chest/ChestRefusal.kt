package zel.wired.chest

import zel.protocol.generated.WiredValidationErrorOut
import zel.protocol.generated.WiredValidationErrorOutCodec
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane

/** Shows the player the client's wired error text [key]. */
internal fun RoomScope.refuseChestUse(
    playerId: Int,
    key: String,
) = out.send(
    Lane.Now,
    Audience.Player(playerId),
    WiredValidationErrorOutCodec,
    WiredValidationErrorOut(key, emptyList()),
)
