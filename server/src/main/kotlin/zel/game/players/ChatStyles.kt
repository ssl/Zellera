package zel.game.players

import zel.db.Database
import zel.db.write
import zel.game.permissions.Privilege
import zel.protocol.generated.PlayerPurchasableChatStyleChangedOut
import zel.protocol.generated.PlayerPurchasableChatStyleChangedOutCodec
import zel.protocol.outgoing.users.NftChatStyles
import zel.room.chat.ChatStyleRules
import java.sql.Connection

class ChatStyles(
    private val database: Database,
) {
    @Volatile
    private var styleByOffer: Map<Int, Int> = emptyMap()

    fun reload() {
        useOffers(
            database.queries
                .select("SELECT id, offer_id FROM chat_bubbles WHERE offer_id IS NOT NULL")
                .rows {
                    it.getInt("offer_id") to
                        it.getInt("id")
                }.toMap(),
        )
    }

    fun useOffers(styles: Map<Int, Int>) {
        styleByOffer = styles.toMap()
    }

    fun styleOfOffer(offerId: Int): Int? = styleByOffer[offerId]

    fun ownedBy(playerId: Int): List<Int> =
        database.queries
            .select(OWNED_SQL) { int(playerId) }
            .rows { it.getInt("bubble_id") }

    fun has(
        playerId: Int,
        styleId: Int,
    ): Boolean = styleId in ownedBy(playerId)

    fun boughtStyles(player: Player?): List<Int> =
        player?.let { inRange(ownedBy(it.profile.id), SHOP_STYLES) }.orEmpty()

    /** Staff with any colour get the whole collectible range. */
    fun nftStylesFor(player: Player?): List<Int> =
        when {
            player == null -> emptyList()
            player.hasPermission(Privilege.CHAT_ANY_BUBBLE) -> NFT_STYLES.toList()
            else -> inRange(ownedBy(player.profile.id), NFT_STYLES)
        }

    /** The saved bubble follows the same rule as a line in a room ([ChatStyleRules]). */
    fun allows(
        player: Player,
        styleId: Int,
    ): Boolean =
        ChatStyleRules.allows(
            styleId,
            anyStyle = player.hasPermission(Privilege.CHAT_ANY_BUBBLE),
            ambassador = player.hasPermission(Privilege.AMBASSADOR_TOOLS),
        ) { has(player.profile.id, it) }

    fun tellGranted(
        player: Player?,
        styleId: Int,
    ) {
        val client = player?.client ?: return
        client.send(PlayerPurchasableChatStyleChangedOutCodec, PlayerPurchasableChatStyleChangedOut(true, styleId))
        if (styleId in NFT_STYLES) client.send(NftChatStyles(nftStylesFor(player)))
    }

    companion object {
        val NFT_STYLES = 1000..1999
        val SHOP_STYLES = 10000..10999
        private const val OWNED_SQL = "SELECT bubble_id FROM player_chat_bubbles WHERE player_id = ? ORDER BY bubble_id"
        private const val GRANT =
            "INSERT IGNORE INTO player_chat_bubbles (player_id, bubble_id, acquired_at) VALUES (?, ?, ?)"

        /** Runs in the caller's transaction; false when the style was owned already. */
        fun grant(
            connection: Connection,
            playerId: Int,
            styleId: Int,
            at: Int,
        ): Boolean =
            connection.write(GRANT) {
                int(playerId)
                int(styleId)
                int(at)
            } > 0

        fun inRange(
            styles: List<Int>,
            range: IntRange,
        ): List<Int> = styles.filter { it in range }
    }
}
