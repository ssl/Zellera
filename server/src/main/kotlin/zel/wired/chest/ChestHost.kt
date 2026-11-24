package zel.wired.chest

import zel.game.items.Item
import zel.protocol.generated.OutgoingCodec

interface ChestHost :
    TableFurni,
    PlayerWallet,
    EarningNotice {
    fun isOnline(playerId: Int): Boolean

    fun name(playerId: Int): String

    /** The one trade rule (trade pass, no running trade lock): online from the standing, offline from its row. */
    fun mayTrade(playerId: Int): Boolean

    fun progress(
        playerId: Int,
        achievement: String,
        amount: Int,
    )

    /** What counts on the chests-organized event rises to [count]; it never goes down. */
    fun furniChestsInUse(
        playerId: Int,
        count: Int,
    ) = Unit

    fun definition(
        wall: Boolean,
        spriteId: Int,
    ): Item?

    fun spriteOf(baseId: Int): Int

    fun isWall(baseId: Int): Boolean

    fun <T> send(
        playerId: Int,
        codec: OutgoingCodec<T>,
        message: T,
    )

    fun starterTexts(): Pair<String, String>

    /** [text] as the word filter leaves it for what [playerId] writes. */
    fun filtered(
        playerId: Int,
        text: String,
    ): String = text
}
