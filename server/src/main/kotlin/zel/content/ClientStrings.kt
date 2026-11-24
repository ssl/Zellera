package zel.content

import zel.db.Sql
import zel.db.orFallback
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference
import javax.sql.DataSource

object ClientStrings {
    const val CLUB_PRODUCT_PREFIX = "subscription.clubProductPrefix"

    const val CLUB_PRODUCT_NAME = "subscription.clubProductName"

    const val CLUB_OFFER_PERK = "perk.clubOffer"

    const val HALLOWEEN_TRACK = "track.halloween"

    const val CLUB_TYPE = "subscription.clubType"

    const val ASSET_HOST = "asset.host"

    const val ASSET_MIRROR_HOSTS = "asset.mirrorHosts"

    const val LINKED_SITES = "site.linkedSites"

    const val CAMERA_OVERLAY_FOLDER = "asset.cameraOverlays"

    const val ROOM_CONTENT_FILE = "client.roomContentFile"

    const val SERVER_ID = "client.serverId"

    const val IMAGE_ROOT = "asset.imageRoot"

    const val FURNI_PATH = "asset.furniPath"

    const val BADGE_FOLDER = "asset.badgeFolder"

    const val SNOWBALL_PATH = "asset.snowballPath"

    const val BUNDLE_EXTENSION = "asset.bundleExtension"

    const val BUNDLE_MAGIC = "asset.bundleMagic"

    const val ICON_PRODUCT_TYPE = "icon.productType"

    const val ICON_TEXT_PREFIX = "icon.textPrefix"

    const val ICON_USED_ACHIEVEMENT = "achievement.iconUsed"

    const val ICON_COLLECTED_ACHIEVEMENT = "achievement.iconCollected"

    const val ICON_SET_ACHIEVEMENT = "achievement.iconSetCompleted"

    const val CLUB_DANCE_ALIAS = "command.clubDanceAlias"

    const val MARKETPLACE_SALES_ACHIEVEMENT = "achievement.marketplaceSales"

    const val ONLINE_TIME_ACHIEVEMENT = "achievement.onlineTime"

    const val COLLAPSED_CATEGORY = "navigator.collapsedCategory"

    const val ROLE_PLAY_NAME_PREFIX = "navigator.rolePlayName."

    /** Numbered keys, each a list of `name=alias` pairs; the client is told them in key order. */
    const val FURNI_ALIASES_PREFIX = "furni.aliases."

    private val log = LoggerFactory.getLogger(ClientStrings::class.java)

    private val values = AtomicReference<Map<String, String>>(emptyMap())

    operator fun get(name: String): String? = values.get()[name]

    /** Where the client finds a badge's picture, written with its own variable for the image library. */
    fun badgeImage(code: String): String = "\${image.library.url}${get(BADGE_FOLDER).orEmpty()}/$code.png"

    fun withPrefix(prefix: String): List<String> =
        values
            .get()
            .filterKeys { it.startsWith(prefix) }
            .values
            .toList()

    fun snapshot(): Map<String, String> = values.get()

    fun install(strings: Map<String, String>) {
        values.set(strings.toMap())
    }

    /** A failed read keeps the strings already installed. */
    fun load(dataSource: DataSource) {
        val read =
            log.orFallback("reading the client profile", null) {
                Sql(dataSource).select("SELECT name, value FROM client_strings").rows {
                    it.getString(1) to
                        it.getString(2)
                }
            } ?: return
        install(read.toMap())
        log.info("Client profile: {} strings", read.size)
    }
}
