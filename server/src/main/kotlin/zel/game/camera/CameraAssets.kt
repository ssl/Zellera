package zel.game.camera

import com.google.gson.JsonParser
import zel.content.ClientStrings
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

private val log = LoggerFactory.getLogger(CameraAssets::class.java)

class CameraAssets(
    private val source: (path: String) -> ByteArray?,
    private val locations: Locations,
    furnidata: ByteArray?,
    pets: Collection<String> = emptyList(),
) {
    data class Locations(
        val furni: String,
        val figure: String,
        val imageLibrary: String,
        val roomContent: String,
    )

    /** What one frame may still fetch: bundles neither loaded nor known missing. One per frame, one thread. */
    internal class Fetches(
        private var left: Int = MAX_FRAME_FETCHES,
    ) {
        fun take(): Boolean = (left > 0).also { if (it) left-- }
    }

    private val furniRevisions = furniRevisions(furnidata)
    private val figureLibraries = figureLibraries(source(locations.figure + "figuremap.xml"))
    private val effectLibraries = effectLibraries(source(locations.figure + "effectmap.xml"))

    /**
     * The libraries of the figure folder a furni-style name may come from: what the game's own gamedata names (the
     * figure and effect maps, its pet types) and the base avatar libraries. Any other name is never fetched.
     */
    private val folderLibraries: Set<String> =
        figureLibraries.values.toSet() + effectLibraries.values + CameraAssetData.baseLibraries() + pets

    // bundles, missing and files are guarded by this object; fetching happens outside the lock.
    private val bundles = RecentlyUsed<AssetBundle>(MAX_BUNDLES)
    private val missing = RecentlyUsed<Unit>(MAX_MISSING)
    private val files = HashMap<String, ByteArray?>()

    internal fun sprite(
        name: String,
        fetches: Fetches = Fetches(),
    ): AssetBundle.Image? {
        if (!SAFE_NAME.matches(name)) return null
        return orNull({ log.debug("Camera asset {}: {}", name, it.message) }) {
            bundleOfSprite(name, fetches)?.image(name)
        }
    }

    private fun bundleOfSprite(
        name: String,
        fetches: Fetches,
    ): AssetBundle? =
        FIGURE.matchEntire(name)?.let { figureBundle(it, name, fetches) }
            ?: LIBRARY.matchEntire(name)?.let { libraryBundle(it.groupValues[1], fetches) }
            ?: bundle(locations.roomContent, fetches)?.takeIf { it.has(name) }

    private fun figureBundle(
        figure: MatchResult,
        name: String,
        fetches: Fetches,
    ): AssetBundle? {
        val (type, typeSuffix, id) = figure.destructured
        val isEffect = type == EFFECT_TYPE
        val library =
            when {
                isEffect && typeSuffix.isNotEmpty() -> effectLibraries[typeSuffix]
                else -> figureLibraries["$type$typeSuffix:$id"]
            } ?: effectLibraries[id].takeIf { isEffect }
        val mapped = library?.let { figureLibrary(it, fetches) }
        if (mapped != null && mapped.has(name)) return mapped
        // Base parts missing from the figure map (sleeves, hrb next to hr) live in the base avatar libraries.
        val parent = PARENT_TYPES[type]?.let { figureLibraries["$it:$id"] }
        return (listOfNotNull(parent) + CameraAssetData.baseLibraries()).firstNotNullOfOrNull { fallback ->
            figureLibrary(fallback, fetches)?.takeIf { it.has(name) }
        }
    }

    private fun figureLibrary(
        library: String,
        fetches: Fetches,
    ): AssetBundle? = bundle("${locations.figure}$library${AssetBundle.extension()}", fetches)

    /** A furni of the furnidata, else a library the gamedata names in the figure folder; else none, unfetched. */
    private fun libraryBundle(
        library: String,
        fetches: Fetches,
    ): AssetBundle? {
        val revision = furniRevisions[library]
        return when {
            revision != null -> bundle("${locations.furni}$revision/$library${AssetBundle.extension()}", fetches)
            library in folderLibraries -> figureLibrary(library, fetches)
            else -> null
        }
    }

    internal fun palette(
        spriteName: String,
        palette: String,
        fetches: Fetches = Fetches(),
    ): IntArray? {
        if (!SAFE_NAME.matches(palette)) return null
        return orNull({ log.debug("Camera palette {} of {}: {}", palette, spriteName, it.message) }) {
            LIBRARY.matchEntire(spriteName)?.let { libraryBundle(it.groupValues[1], fetches) }?.palette(palette)
        }
    }

    internal fun roomAsset(
        name: String,
        fetches: Fetches = Fetches(),
    ): AssetBundle.Image? {
        if (!SAFE_NAME.matches(name)) return null
        return orNull { bundle(locations.roomContent, fetches)?.image(name) }
    }

    fun filterImage(name: String): BufferedImage? {
        if (!SAFE_NAME.matches(name)) return null
        val folder = ClientStrings[ClientStrings.CAMERA_OVERLAY_FOLDER] ?: return null
        return orNull { cachedFile("${locations.imageLibrary}$folder/$name.png")?.let { AssetBundle.readPng(it) } }
    }

    private fun cachedFile(path: String): ByteArray? {
        synchronized(this) {
            if (path in files) return files[path]
        }
        val bytes = source(path)
        return synchronized(this) { files.getOrPut(path) { bytes } }
    }

    /**
     * null when there is none; a fetch that fails (not a missing file) is tried again next time, and so is one the
     * frame had no [Fetches] left for.
     */
    private fun bundle(
        path: String,
        fetches: Fetches,
    ): AssetBundle? {
        synchronized(this) {
            if (path in missing) return null
            bundles[path]?.let { return it }
        }
        return if (fetches.take()) fetched(path) else null
    }

    private fun fetched(path: String): AssetBundle? {
        val loaded = source(path)?.let { readBundle(path, it) }
        return synchronized(this) {
            if (loaded == null) {
                missing[path] = Unit
                null
            } else {
                bundles.getOrPut(path) { loaded }
            }
        }
    }

    private fun readBundle(
        path: String,
        bytes: ByteArray,
    ): AssetBundle? =
        orNull({ log.debug("Camera library {} unreadable: {}", path, it.message) }) {
            AssetBundle.parse(bytes).also { loaded ->
                if (path.startsWith(locations.figure)) loaded.greyShades = CameraAssetData.shadesOf(loaded.name)
            }
        }

    private class RecentlyUsed<V>(
        private val limit: Int,
    ) : LinkedHashMap<String, V>(INITIAL_CAPACITY, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, V>?): Boolean = size > limit
    }

    companion object {
        private val FIGURE = Regex("^(?:h|sh)_[a-z]+_([a-z]+)(\\d*)_(\\d+)_\\d+_\\d+$")

        private val LIBRARY = Regex("^(.+)_(?:64|32)_(?:[a-z]{1,2}|sd)_\\d+_\\d+(?:_\\d+)?$")
        private val SAFE_NAME = Regex("^[A-Za-z0-9_\\-]{1,120}$")
        private const val EFFECT_TYPE = "fx"
        private const val PET_TYPES = "pet.configuration"
        private const val MAX_BUNDLES = 400

        /**
         * Bundles one frame may fetch: as many as the cache keeps (a frame past that would only push out its own),
         * far more than a typical photo needs with an empty cache.
         */
        internal const val MAX_FRAME_FETCHES = MAX_BUNDLES
        private const val INITIAL_CAPACITY = 64
        private const val LOAD_FACTOR = 0.75f
        internal const val MAX_MISSING = 4096

        private val PARENT_TYPES = mapOf("hrb" to "hr", "ls" to "ch", "rs" to "ch", "lc" to "ch", "rc" to "ch")

        internal fun figurePart(name: String): Boolean = FIGURE.matches(name)

        /** The pet types the external variables list (`pet.configuration`); their libraries are named so. */
        fun pets(variables: Map<String, String>): Set<String> =
            variables[PET_TYPES]
                ?.split(',')
                ?.map(String::trim)
                ?.filter { it.isNotEmpty() && SAFE_NAME.matches(it) }
                ?.toSet()
                .orEmpty()

        @Suppress("TooGenericExceptionCaught") // a frame names any asset: one broken file must not refuse the frame
        private inline fun <T : Any> orNull(
            onFailure: (Exception) -> Unit = {},
            lookup: () -> T?,
        ): T? =
            try {
                lookup()
            } catch (e: IOException) {
                onFailure(e)
                null
            } catch (e: RuntimeException) {
                onFailure(e)
                null
            }

        /** The first entry of a class name wins. */
        private fun furniRevisions(furnidata: ByteArray?): Map<String, Int> {
            if (furnidata == null) return emptyMap()
            val root = JsonParser.parseString(furnidata.toString(Charsets.UTF_8)).asJsonObject
            return listOf("roomitemtypes", "wallitemtypes")
                .filter(root::has)
                .flatMap { kind -> root.getAsJsonObject(kind).getAsJsonArray("furnitype").map { it.asJsonObject } }
                .filter { it.has("classname") && it.has("revision") }
                .groupingBy { it["classname"].asString.substringBefore('*') }
                .aggregate { _, first: Int?, furni, _ -> first ?: furni["revision"].asInt }
        }

        private fun figureLibraries(xml: ByteArray?): Map<String, String> {
            if (xml == null) return emptyMap()
            return buildMap {
                for (lib in parseXml(xml).getElementsByTagName("lib").elements()) {
                    for (part in lib.getElementsByTagName("part").elements()) {
                        putIfAbsent("${part.getAttribute("type")}:${part.getAttribute("id")}", lib.getAttribute("id"))
                    }
                }
            }
        }

        private fun effectLibraries(xml: ByteArray?): Map<String, String> {
            if (xml == null) return emptyMap()
            return buildMap {
                for (effect in parseXml(xml).getElementsByTagName("effect").elements()) {
                    putIfAbsent(effect.getAttribute("id"), effect.getAttribute("lib"))
                }
            }
        }

        private fun NodeList.elements(): List<Element> = List(length) { item(it) as Element }

        @Suppress("TooGenericExceptionCaught") // whatever the XML parser throws means the same: an unreadable file
        private fun parseXml(xml: ByteArray): Document =
            try {
                val factory = DocumentBuilderFactory.newInstance()
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                factory.isExpandEntityReferences = false
                factory.newDocumentBuilder().parse(InputSource(StringReader(xml.toString(Charsets.UTF_8))))
            } catch (e: Exception) {
                throw IOException(e)
            }
    }
}
