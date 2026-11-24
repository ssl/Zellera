package zel.game.items

import zel.content.ClientStrings
import zel.core.Configuration

object ItemDataUrls {
    const val SITE_SETTING = "site.url"

    private const val SCHEME = "(?:https?:)?//"

    /** The host is group 1. */
    private val WEB_LINK = Regex("^(?:https?:)?//([^/?#\\s]+)", RegexOption.IGNORE_CASE)

    private val LOOPBACK =
        Regex(
            "^(?:localhost|[^/?#\\s]*\\.localhost|127(?:\\.\\d{1,3}){3}|\\[::1]|0\\.0\\.0\\.0)(?::\\d+)?$",
            RegexOption.IGNORE_CASE,
        )

    private val OWN_IMAGE_PATH =
        Regex(
            "^/assets/[A-Za-z0-9_\\-][A-Za-z0-9_\\-./]*\\.(?:png|gif|jpe?g)(?:\\?[A-Za-z0-9_=&.\\-]*)?$",
            RegexOption.IGNORE_CASE,
        )

    private class ImageRewrite(
        val prefix: Regex,
        val ownPath: String,
        val skipWhen: String? = null,
    ) {
        fun apply(url: String): String? {
            if (skipWhen != null && skipWhen in url) return null
            val head = prefix.find(url) ?: return null
            return head.groupValues[1] + ownPath + url.substring(head.range.last + 1)
        }
    }

    private class PackHosts(
        val pack: Map<String, String>,
    ) {
        val imageRewrites: List<ImageRewrite> =
            listOfNotNull(
                pack[ClientStrings.ASSET_HOST]?.let {
                    ImageRewrite(lineStart(Regex.escape(it) + "/"), "/assets/", skipWhen = "/servercamera/")
                },
                pack[ClientStrings.ASSET_MIRROR_HOSTS]?.let { hosts ->
                    pack[ClientStrings.IMAGE_ROOT]?.let { ImageRewrite(lineStart("(?:$hosts)/$it/"), "/assets/$it/") }
                },
            )

        val linkKinds: List<Pair<Regex, String>> =
            pack[ClientStrings.LINKED_SITES]
                ?.let {
                    listOf(
                        insensitive("$SCHEME(?:$it)/shop") to "/shop",
                        insensitive("$SCHEME(?:$it)/community/article/") to "/news",
                    )
                }.orEmpty()

        private fun lineStart(tail: String) = insensitive("^(\\s*)$SCHEME$tail")

        private fun insensitive(text: String) = Regex(text, RegexOption.IGNORE_CASE)
    }

    @Volatile
    private var hosts = PackHosts(emptyMap())

    private fun hosts(): PackHosts {
        val pack = ClientStrings.snapshot()
        return hosts.takeIf { it.pack === pack } ?: PackHosts(pack).also { hosts = it }
    }

    fun site(config: Configuration?): String = config?.let { normalizeSite(it.getValue(SITE_SETTING, "")) }.orEmpty()

    fun normalizeSite(site: String?): String = site.orEmpty().trim().trimEnd('/')

    fun image(
        url: String?,
        site: String?,
    ): String? = url?.let { image(it, site) }

    @JvmName("imageOf")
    fun image(
        url: String,
        site: String?,
    ): String {
        if (url.isEmpty()) return url
        return hosts().imageRewrites.firstNotNullOfOrNull { it.apply(url) } ?: pathOnOwnOrigin(url, site)
    }

    fun adImage(
        url: String?,
        site: String?,
    ): String? {
        val path = url?.trim()?.takeIf { it.isNotEmpty() }?.let { image(it, site) } ?: return null
        return path.takeIf { ".." !in it && "//" !in it && OWN_IMAGE_PATH.matches(it) }
    }

    private fun pathOnOwnOrigin(
        url: String,
        site: String?,
    ): String {
        val blanks = url.takeWhile { it.isWhitespace() }
        val head = WEB_LINK.find(url.substring(blanks.length)) ?: return url
        val own = normalizeSite(site)
        val host = head.groupValues[1]
        if (!isLocal(host) && (own.isEmpty() || !host.equals(hostOf(own), ignoreCase = true))) return url
        return blanks + rooted(url.substring(blanks.length + head.range.last + 1))
    }

    private fun rooted(path: String) = if (path.startsWith("/")) path else "/$path"

    fun isLocal(host: String?): Boolean = host != null && LOOPBACK.matches(host.trim())

    fun link(
        url: String?,
        site: String?,
    ): String? {
        if (url.isNullOrBlank()) return url
        val text = url.trim()
        val head = WEB_LINK.find(text) ?: return url
        val own = normalizeSite(site)
        val host = head.groupValues[1]
        return when {
            own.isEmpty() -> ""
            host.equals(hostOf(own), ignoreCase = true) -> url
            isLocal(host) -> own + rooted(text.substring(head.range.last + 1))
            else -> own + (hosts().linkKinds.firstOrNull { (shape, _) -> shape.matchesAt(text, 0) }?.second ?: "/help")
        }
    }

    private fun hostOf(site: String): String = WEB_LINK.find(site)?.groupValues?.get(1) ?: site
}
