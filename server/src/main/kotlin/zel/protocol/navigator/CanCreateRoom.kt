package zel.protocol.navigator

import zel.protocol.generated.CanCreateRoomOut
import zel.protocol.generated.CanCreateRoomOutCodec
import zel.protocol.outgoing.OutgoingMessage

class CanCreateRoom(
    private val count: Int,
    private val max: Int,
) : OutgoingMessage<CanCreateRoomOut>(CanCreateRoomOutCodec) {
    override fun message() = CanCreateRoomOut(if (count >= max) 1 else 0, max)
}
