package zel.game.items

import com.google.gson.Gson
import com.google.gson.JsonParseException
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.format.DateTimeParseException
import kotlin.text.Charsets.UTF_8

class YoutubeApi(
    private val key: String,
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build(),
) {
    /** Every field may be missing in the reply. */
    private class Listing(
        val items: List<Entry>? = null,
        val nextPageToken: String? = null,
    )

    private class Entry(
        val id: String? = null,
        val snippet: Snippet? = null,
        val contentDetails: Details? = null,
    ) {
        fun lengthSeconds(): Int = durationSeconds(contentDetails?.duration.orEmpty())
    }

    private class Snippet(
        val title: String? = null,
        val description: String? = null,
    )

    private class Details(
        val videoId: String? = null,
        val duration: String? = null,
    )

    fun playlist(code: String): YoutubePlaylist? {
        val about =
            call("playlists", mapOf("part" to "snippet", "id" to code))?.items?.firstOrNull()?.snippet ?: return null
        val videoIds =
            everyPage("playlistItems", mapOf("part" to "contentDetails", "playlistId" to code))
                .mapNotNull { it.contentDetails?.videoId }
                .distinct()
        val lengths = videoIds.chunked(IDS_PER_CALL).flatMap(::lengthsOf).toMap()
        val clips = playable(videoIds, lengths).ifEmpty { return null }
        return YoutubePlaylist(code, about.title.orEmpty(), about.description.orEmpty(), clips)
    }

    private fun lengthsOf(videoIds: List<String>): List<Pair<String, Int>> =
        call("videos", mapOf("part" to "contentDetails", "id" to videoIds.joinToString(",")))
            ?.items
            .orEmpty()
            .mapNotNull { entry -> entry.id?.let { it to entry.lengthSeconds() } }

    private fun everyPage(
        resource: String,
        params: Map<String, String>,
    ): List<Entry> {
        val sized = params + ("maxResults" to IDS_PER_CALL.toString())
        // A generated mix answers a next page forever, repeating its videos.
        return generateSequence(call(resource, sized)) { previous ->
            previous.nextPageToken?.let { call(resource, sized + ("pageToken" to it)) }
        }.take(MAX_PAGES).flatMap { it.items.orEmpty() }.toList()
    }

    private fun call(
        resource: String,
        params: Map<String, String>,
    ): Listing? {
        val sent =
            HttpRequest
                .newBuilder(uriOf(resource, params + ("key" to key)))
                .timeout(READ_TIMEOUT)
                .GET()
                .build()
        return try {
            val reply = client.send(sent, HttpResponse.BodyHandlers.ofString(UTF_8))
            if (reply.statusCode() == HTTP_OK) {
                GSON.fromJson(reply.body(), Listing::class.java)
            } else {
                log.warn("YouTube {} answered {}", resource, reply.statusCode())
                null
            }
        } catch (unreachable: IOException) {
            log.warn("YouTube {} not reached: {}", resource, unreachable.toString())
            null
        } catch (notJson: JsonParseException) {
            log.warn("YouTube {} answered no JSON: {}", resource, notJson.toString())
            null
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        }
    }

    private fun uriOf(
        resource: String,
        params: Map<String, String>,
    ): URI {
        val pairs = params.map { (name, value) -> "$name=${URLEncoder.encode(value, UTF_8)}" }
        return URI.create(BASE + resource + "?" + pairs.joinToString("&"))
    }

    companion object {
        private val log = LoggerFactory.getLogger(YoutubeApi::class.java)
        private val GSON = Gson()
        private const val BASE = "https://www.googleapis.com/youtube/v3/"
        private const val HTTP_OK = 200
        private const val IDS_PER_CALL = 50
        private const val MAX_PAGES = 20
        private val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(10)
        private val READ_TIMEOUT: Duration = Duration.ofSeconds(20)

        /** The videos with a length: a private or deleted video has none, a live stream lasts 0 seconds. */
        internal fun playable(
            videoIds: List<String>,
            lengths: Map<String, Int>,
        ): List<YoutubePlaylist.Clip> =
            videoIds.mapNotNull { id -> lengths[id]?.takeIf { it >= 1 }?.let { YoutubePlaylist.Clip(id, it) } }

        fun durationSeconds(iso: String): Int =
            try {
                Duration.parse(iso).seconds.toInt()
            } catch (notALength: DateTimeParseException) {
                0
            }
    }
}
