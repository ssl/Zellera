package zel.protocol.incoming.catalog

import zel.protocol.generated.PurchaseFromCatalogAsGiftIn

internal class GiftRequest(
    m: PurchaseFromCatalogAsGiftIn,
    messageMax: Int,
    filter: (String) -> String,
) {
    val pageId: Int = m.int1
    val itemId: Int = m.int2
    val extraData: String = m.string1.take(EXTRADATA_MAX)
    val username: String = m.string2.take(USERNAME_MAX)
    val message: String = filter(m.string3.take(messageMax))
    val spriteId: Int = m.int3
    val color: Int = m.int4
    val ribbonId: Int = m.int5
    val showName: Boolean = m.boolean1

    private companion object {
        const val USERNAME_MAX = 32
        const val EXTRADATA_MAX = 256
    }
}
