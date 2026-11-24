package zel.game.claims

import zel.db.Database
import zel.game.items.Items
import zel.protocol.claims.ClaimMessages
import zel.protocol.claims.ClaimPackets
import zel.protocol.typed.PacketPackage
import java.util.function.IntSupplier

class ClaimsModule(
    database: Database,
    items: Items,
    unixTime: IntSupplier,
) {
    private val messages = ClaimMessages()
    val claims = ProductClaims(ClaimableProducts(database), items, messages, unixTime)
    val packets: PacketPackage = ClaimPackets(claims, messages)
}
