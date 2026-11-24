package zel.protocol.outgoing.polls

import zel.game.polls.Poll
import zel.protocol.generated.PollOfferOut
import zel.protocol.generated.PollOfferOutCodec
import zel.protocol.outgoing.OutgoingMessage

class PollOffer(
    val poll: Poll,
) : OutgoingMessage<PollOfferOut>(PollOfferOutCodec) {
    override fun message() = PollOfferOut(poll.id, poll.type, poll.offerHeadline(), poll.summary)
}
