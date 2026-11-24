package zel.protocol.outgoing.users

import zel.game.players.Player
import zel.protocol.generated.FigureSetIdsOut
import zel.protocol.generated.FigureSetIdsOutCodec
import zel.protocol.outgoing.OutgoingMessage

class FigureSetIds(
    private val player: Player,
) : OutgoingMessage<FigureSetIdsOut>(FigureSetIdsOutCodec) {
    override fun message(): FigureSetIdsOut {
        val clothingSets = checkNotNull(player.services.game.catalog).contents.clothingSets
        val owned =
            player.inventory.wardrobe.clothing
                .mapNotNull { clothingSets[it] }
        return FigureSetIdsOut(owned.flatMap { it.setId.asList() }, owned.map { it.name.orEmpty() })
    }
}
