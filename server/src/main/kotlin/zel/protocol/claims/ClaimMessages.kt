package zel.protocol.claims

import zel.net.clients.ClientConnection
import zel.protocol.generated.ClaimProductResultOut
import zel.protocol.generated.ClaimProductResultOutCodec
import zel.protocol.generated.HasClaimedProductResponseOut
import zel.protocol.generated.HasClaimedProductResponseOutCodec

class ClaimMessages {
    fun result(
        client: ClientConnection,
        claimId: String,
        result: Int,
    ) = client.send(ClaimProductResultOutCodec, ClaimProductResultOut(claimId, result))

    fun hasClaimed(
        client: ClientConnection,
        claimId: String,
        claimed: Boolean,
    ) = client.send(
        HasClaimedProductResponseOutCodec,
        HasClaimedProductResponseOut(claimId, claimed),
    )
}
