package zel.room.side

import zel.game.items.YoutubePlaylist
import zel.game.items.YoutubePlaylists
import zel.protocol.generated.YoutubeControlVideoOut
import zel.protocol.generated.YoutubeControlVideoOutCodec
import zel.protocol.generated.YoutubeDisplayPlaylistsOut
import zel.protocol.generated.YoutubeDisplayPlaylistsOutCodec
import zel.protocol.generated.YoutubeDisplayVideoOut
import zel.protocol.generated.YoutubeDisplayVideoOutCodec
import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.util.MILLIS_PER_SECOND
import java.util.WeakHashMap

class YoutubeDisplays(
    private val playlists: () -> YoutubePlaylists?,
) {
    private data class Playing(
        val playlist: YoutubePlaylist,
        val video: Int,
        val startedAt: Long,
        val offset: Int,
        val playing: Boolean,
    )

    private val rooms = WeakHashMap<Room, MutableMap<Int, Playing>>()

    private fun of(room: Room): MutableMap<Int, Playing> = synchronized(rooms) { rooms.getOrPut(room) { HashMap() } }

    fun status(
        scope: RoomScope,
        playerId: Int,
        furniId: Int,
    ) {
        tv(scope, furniId) ?: return
        val listed = listsOf()
        if (listed.isEmpty()) return
        val playing = of(scope.room)[furniId]
        val list = listed.map { YoutubeDisplayPlaylistsOut.List1Item(it.id, it.name, it.description) }
        val answer = YoutubeDisplayPlaylistsOut(furniId, list, playing?.playlist?.id.orEmpty())
        val to = Audience.Player(playerId)
        scope.out.send(Lane.Now, to, YoutubeDisplayPlaylistsOutCodec, answer)
        if (playing != null) scope.out.send(Lane.Now, to, YoutubeDisplayVideoOutCodec, video(scope, furniId, playing))
    }

    fun choose(
        scope: RoomScope,
        furniId: Int,
        playlistId: String,
    ) {
        tv(scope, furniId) ?: return
        val playlist = listsOf().firstOrNull { it.id == playlistId && it.clips.isNotEmpty() } ?: return
        val playing = Playing(playlist, 0, scope.now(), 0, true)
        of(scope.room)[furniId] = playing
        scope.out.send(Lane.Now, Audience.Room, YoutubeDisplayVideoOutCodec, video(scope, furniId, playing))
    }

    fun control(
        scope: RoomScope,
        furniId: Int,
        state: Int,
    ) {
        val playing = of(scope.room)[furniId] ?: return
        val size = playing.playlist.clips.size
        val next =
            when (state) {
                NEXT -> {
                    playing.copy(
                        video = (playing.video + 1) % size,
                        startedAt = scope.now(),
                        offset = 0,
                        playing = true,
                    )
                }

                PREVIOUS -> {
                    playing.copy(
                        video = (playing.video - 1 + size) % size,
                        startedAt = scope.now(),
                        offset = 0,
                        playing = true,
                    )
                }

                PAUSE -> {
                    playing.copy(offset = positionOf(scope, playing), playing = false)
                }

                RESUME -> {
                    if (playing.playing) return
                    playing.copy(startedAt = scope.now(), playing = true)
                }

                else -> {
                    return
                }
            }
        of(scope.room)[furniId] = next
        if (state == NEXT || state == PREVIOUS) {
            scope.out.send(Lane.Now, Audience.Room, YoutubeDisplayVideoOutCodec, video(scope, furniId, next))
        } else {
            val shown = YoutubeControlVideoOut(furniId, if (next.playing) SHOWN_PLAYING else SHOWN_PAUSED)
            scope.out.send(Lane.Now, Audience.Room, YoutubeControlVideoOutCodec, shown)
        }
    }

    private fun positionOf(
        scope: RoomScope,
        playing: Playing,
    ): Int =
        playing.offset + if (playing.playing) ((scope.now() - playing.startedAt) / MILLIS_PER_SECOND).toInt() else 0

    private fun video(
        scope: RoomScope,
        furniId: Int,
        playing: Playing,
    ): YoutubeDisplayVideoOut {
        val clip = playing.playlist.clips[playing.video]
        return YoutubeDisplayVideoOut(
            furniId,
            clip.id,
            positionOf(scope, playing),
            clip.seconds,
            if (playing.playing) SHOWN_PLAYING else SHOWN_PAUSED,
        )
    }

    private fun tv(
        scope: RoomScope,
        furniId: Int,
    ): RoomFurni? = scope.room.furni[furniId]?.takeIf { Interactions.of(it.definition) == YOUTUBE }

    private fun listsOf(): List<YoutubePlaylist> = playlists()?.all().orEmpty()

    private companion object {
        const val YOUTUBE = "display.video"
        const val PREVIOUS = 0
        const val NEXT = 1
        const val PAUSE = 2
        const val RESUME = 3
        const val SHOWN_PLAYING = 1
        const val SHOWN_PAUSED = 2
    }
}
