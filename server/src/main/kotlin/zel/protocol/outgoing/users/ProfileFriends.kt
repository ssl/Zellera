package zel.protocol.outgoing.users

import zel.game.messenger.RelationSummary
import zel.protocol.generated.RelationshipStatusInfoOut
import zel.protocol.generated.RelationshipStatusInfoOutCodec
import zel.protocol.outgoing.OutgoingMessage

class ProfileFriends(
    private val userId: Int,
    private val relations: List<RelationSummary>,
) : OutgoingMessage<RelationshipStatusInfoOut>(RelationshipStatusInfoOutCodec) {
    override fun message() =
        RelationshipStatusInfoOut(
            userId,
            relations.map {
                RelationshipStatusInfoOut.List1Item(it.relation, it.friends, it.shownId, it.shownName, it.shownLook)
            },
        )
}
