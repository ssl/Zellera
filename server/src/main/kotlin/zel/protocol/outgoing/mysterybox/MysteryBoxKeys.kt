package zel.protocol.outgoing.mysterybox

import zel.game.items.mysterybox.BoxColour
import zel.protocol.generated.MysteryBoxKeysOut
import zel.protocol.generated.MysteryBoxKeysOutCodec
import zel.protocol.outgoing.OutgoingMessage

class MysteryBoxKeys(
    boxColour: String?,
    keyColour: String?,
) : OutgoingMessage<MysteryBoxKeysOut>(MysteryBoxKeysOutCodec) {
    private val box = BoxColour.named(boxColour)?.code.orEmpty()
    private val key = BoxColour.named(keyColour)?.code.orEmpty()

    override fun message() = MysteryBoxKeysOut(box, key)
}
