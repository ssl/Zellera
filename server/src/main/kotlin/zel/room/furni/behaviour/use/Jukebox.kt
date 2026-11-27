package zel.room.furni.behaviour.use

import zel.game.achievements.GameEvents
import zel.game.items.Item
import zel.game.items.ItemInteraction
import zel.game.items.Items
import zel.game.items.OwnedFurni
import zel.game.players.Player
import zel.protocol.generated.FurniListInvalidateOut
import zel.protocol.generated.FurniListInvalidateOutCodec
import zel.protocol.generated.FurniListRemoveOut
import zel.protocol.generated.FurniListRemoveOutCodec
import zel.protocol.generated.JukeboxPlayListFullOut
import zel.protocol.generated.JukeboxPlayListFullOutCodec
import zel.protocol.generated.JukeboxSongDisksOut
import zel.protocol.generated.JukeboxSongDisksOutCodec
import zel.protocol.generated.NowPlayingOut
import zel.protocol.generated.NowPlayingOutCodec
import zel.protocol.generated.PlayerSongDisksInventoryOut
import zel.protocol.generated.PlayerSongDisksInventoryOutCodec
import zel.protocol.generated.UnseenItemsOut
import zel.protocol.generated.UnseenItemsOutCodec
import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.RoomWrites
import zel.room.entry.DoorCheck
import zel.room.entry.EntryViews
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.furni.behaviour.state.StateShow
import zel.room.furni.place.Placeable
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.tick.TickPart
import zel.util.MILLIS_PER_MINUTE
import zel.util.MILLIS_PER_SECOND
import java.util.concurrent.ConcurrentHashMap

class Jukebox(
    private val items: () -> Items?,
    private val store: JukeboxStore,
    private val player: (Int) -> Player?,
    private val writes: RoomWrites,
    private val progress: (playerId: Int, event: String, amount: Int) -> Unit = { _, _, _ -> },
    private val capacity: (Item) -> Int = { Int.MAX_VALUE },
    private val levelOf: (RoomScope, Int) -> Int = { _, _ -> 0 },
) : RoomPart,
    Usable,
    Placeable {
    data class Disc(
        val itemId: Int,
        val songId: Int,
        val seconds: Int,
        val ownerId: Int,
    )

    private class State {
        val discs = ArrayList<Disc>()
        var playing = false
        var index = 0
        var startedAt = 0L

        /** On since before the room loaded: its song starts at the room's first tick or answer. */
        var resumed = false

        /** The player who switched it on, whom the playing time counts for; null while it is off. */
        var listenedFor: Int? = null
        var switchedOnAt = 0L
    }

    private val rooms = ConcurrentHashMap<Room, State>()

    override fun load(room: Room) {
        val state = State()
        store.discs(room.info.id).forEach { state.discs += Disc(it.itemId, it.songId, seconds(it.songId), it.ownerId) }
        val on = room.furni.all().filter { it.behaviour === this && it.state == ON }
        if (on.isNotEmpty() && state.discs.isNotEmpty()) {
            state.playing = true
            state.resumed = true
        } else {
            on.forEach { StateShow.set(it, OFF) }
        }
        rooms[room] = state
    }

    /** A jukebox that is on stays on, so it plays again when its room loads again. */
    override fun unload(scope: RoomScope) {
        val state = rooms.remove(scope.room) ?: return
        played(scope, state)
    }

    val tick: TickPart =
        TickPart { scope ->
            val state = rooms[scope.room]
            if (state != null && state.playing && state.discs.isNotEmpty()) {
                val song = state.discs[state.index.coerceAtMost(state.discs.lastIndex)]
                if (scope.now() - startedAt(scope, state) >= song.seconds * MILLIS_PER_SECOND) {
                    state.index = (state.index + 1) % state.discs.size
                    state.startedAt = scope.now()
                    nowPlaying(scope, state)
                }
            }
        }

    /**
     * The client sends [EDITOR_OPENED] each time the furni's owner opens the playlist editor (it opens the editor
     * itself, only for the owner), -2 when someone else with rights double-clicks, and the editor's play button a
     * playlist index. The editor's open gets no answer; any other value switches the jukebox on or off. An empty
     * jukebox does not switch on and gets no answer.
     */
    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        if (param == EDITOR_OPENED) return
        if (levelOf(scope, playerId) < DoorCheck.RIGHTS) return
        val state = rooms[scope.room] ?: return
        if (!state.playing && state.discs.isEmpty()) return
        if (state.playing) played(scope, state)
        state.playing = !state.playing
        state.index = 0
        state.startedAt = scope.now()
        state.resumed = false
        StateShow.data(scope, furni, if (state.playing) ON else OFF)
        if (state.playing) {
            state.listenedFor = playerId
            state.switchedOnAt = scope.now()
        }
        nowPlaying(scope, state)
    }

    private fun played(
        scope: RoomScope,
        state: State,
    ) {
        val starter = state.listenedFor ?: return
        val minutes = ((scope.now() - state.switchedOnAt) / MILLIS_PER_MINUTE).toInt()
        if (minutes > 0) progress(starter, GameEvents.FURNI_JUKEBOX_PLAYED, minutes)
        state.listenedFor = null
    }

    override fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        val state = rooms[scope.room] ?: return
        val roomId = scope.room.info.id
        played(scope, state)
        state.playing = false
        StateShow.set(furni, OFF)
        val discs = state.discs.toList()
        state.discs.clear()
        writes.write(
            roomId,
            Runnable {
                store.removed(roomId, discs.map { it.itemId })
                discs.forEach(::giveBack)
            },
        )
        lists(scope, state, furni)
        nowPlaying(scope, state)
    }

    /** GetJukeboxPlayList: the room's playlist, to the asker only. */
    fun playList(
        scope: RoomScope,
        playerId: Int,
    ) {
        val state = rooms[scope.room] ?: return
        scope.out.send(Lane.Now, Audience.Player(playerId), JukeboxSongDisksOutCodec, disks(state, box(scope)))
    }

    /** GetPlayerSongDisks: the asker's own discs, to the asker only. */
    fun songDisks(
        scope: RoomScope,
        playerId: Int,
    ) {
        if (rooms[scope.room] == null) return
        scope.out.send(Lane.Now, Audience.Player(playerId), PlayerSongDisksInventoryOutCodec, inventory(playerId))
    }

    /** GetNowPlaying: what plays now, to the asker only. */
    fun nowPlaying(
        scope: RoomScope,
        playerId: Int,
    ) {
        val state = rooms[scope.room] ?: return
        scope.out.send(Lane.Now, Audience.Player(playerId), NowPlayingOutCodec, playing(scope, state))
    }

    /**
     * A playing jukebox's song goes to each entrant unasked, right after Items and before RoomEntryInfo: NowPlaying
     * with the position into the song. A jukebox that is off sends none.
     */
    fun entered(
        scope: RoomScope,
        playerId: Int,
    ) {
        val state = rooms[scope.room] ?: return
        if (!state.playing || state.discs.isEmpty()) return
        scope.out.send(Lane.Now, Audience.Player(playerId), NowPlayingOutCodec, playing(scope, state))
    }

    fun entryViews(next: EntryViews): EntryViews =
        object : EntryViews by next {
            override fun afterFurni(
                scope: RoomScope,
                playerId: Int,
            ) {
                next.afterFurni(scope, playerId)
                entered(scope, playerId)
            }
        }

    fun add(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        itemId: Int,
    ) {
        val state = rooms[scope.room] ?: return
        if (level < DoorCheck.RIGHTS) return
        val box = box(scope) ?: return
        if (state.discs.size >= capacity(box.definition)) {
            scope.out.send(Lane.Now, Audience.Player(playerId), JukeboxPlayListFullOutCodec, JukeboxPlayListFullOut)
            return
        }
        val owner = player(playerId) ?: return
        val disc =
            owner.inventory.furni[itemId]?.takeIf {
                it.definition.interaction.name ==
                    ItemInteraction.MUSIC_DISC
            }
                ?: return
        owner.inventory.furni.remove(disc)
        state.discs += Disc(disc.id, disc.songId, seconds(disc.songId), playerId)
        val roomId = scope.room.info.id
        val discId = disc.id
        writes.write(roomId, Runnable { store.added(roomId, discId) })
        scope.out.send(Lane.Now, Audience.Room, JukeboxSongDisksOutCodec, disks(state, box))
        scope.out.send(Lane.Now, Audience.Player(playerId), FurniListRemoveOutCodec, FurniListRemoveOut(-discId))
        inventories(scope)
    }

    fun remove(
        scope: RoomScope,
        level: Int,
        index: Int,
    ) {
        val state = rooms[scope.room] ?: return
        if (level < DoorCheck.RIGHTS || index !in state.discs.indices) return
        val disc = state.discs.removeAt(index)
        if (state.playing) nowPlaying(scope, state)
        val roomId = scope.room.info.id
        writes.write(
            roomId,
            Runnable {
                store.removed(roomId, listOf(disc.itemId))
                giveBack(disc)
            },
        )
        scope.out.send(Lane.Now, Audience.Room, JukeboxSongDisksOutCodec, disks(state, box(scope)))
        inventories(scope)
    }

    private fun giveBack(disc: Disc) {
        val owner = player(disc.ownerId) ?: return
        val item: OwnedFurni = items()?.loadFurni(disc.itemId) ?: return
        owner.inventory.furni.put(item)
        val client = owner.client ?: return
        val seen = UnseenItemsOut.CategoriesItem(UNSEEN_FURNI, listOf(disc.itemId))
        client.send(UnseenItemsOutCodec, UnseenItemsOut(listOf(seen)))
        client.send(PlayerSongDisksInventoryOutCodec, inventory(disc.ownerId))
        client.send(FurniListInvalidateOutCodec, FurniListInvalidateOut)
    }

    private fun lists(
        scope: RoomScope,
        state: State,
        box: RoomFurni,
    ) {
        scope.out.send(Lane.Now, Audience.Room, JukeboxSongDisksOutCodec, disks(state, box))
        inventories(scope)
    }

    private fun inventories(scope: RoomScope) {
        scope.room.units.playerIds().forEach { viewer ->
            val list = inventory(viewer)
            scope.out.send(Lane.Now, Audience.Player(viewer), PlayerSongDisksInventoryOutCodec, list)
        }
    }

    private fun box(scope: RoomScope): RoomFurni? =
        scope.room.furni
            .all()
            .firstOrNull { it.behaviour === this }

    /** Leads with the box's playlist capacity, which the client reads as the playlist's max length. */
    private fun disks(
        state: State,
        box: RoomFurni?,
    ): JukeboxSongDisksOut =
        JukeboxSongDisksOut(
            box?.let { capacity(it.definition) } ?: 0,
            state.discs.map { JukeboxSongDisksOut.List1Item(it.itemId, it.songId) },
        )

    private fun inventory(playerId: Int): PlayerSongDisksInventoryOut {
        val discs =
            player(playerId)
                ?.inventory
                ?.furni
                ?.all
                .orEmpty()
                .filter { it.definition.interaction.name == ItemInteraction.MUSIC_DISC }
        return PlayerSongDisksInventoryOut(
            discs.map { PlayerSongDisksInventoryOut.List1Item(it.id, it.songId) },
        )
    }

    private fun nowPlaying(
        scope: RoomScope,
        state: State,
    ) = scope.out.send(Lane.Now, Audience.Room, NowPlayingOutCodec, playing(scope, state))

    private fun playing(
        scope: RoomScope,
        state: State,
    ): NowPlayingOut {
        if (!state.playing || state.discs.isEmpty()) return NowPlayingOut(-1, -1, -1, -1, -1)
        val index = state.index.coerceAtMost(state.discs.lastIndex)
        val song = state.discs[index]
        val nextIndex = (index + 1) % state.discs.size
        val next = state.discs[nextIndex]
        val position = (scope.now() - startedAt(scope, state)).toInt()
        return NowPlayingOut(song.songId, index, next.songId, nextIndex, position)
    }

    private fun startedAt(
        scope: RoomScope,
        state: State,
    ): Long {
        if (state.resumed) {
            state.startedAt = scope.now()
            state.resumed = false
        }
        return state.startedAt
    }

    private fun seconds(songId: Int): Int = items()?.jukeboxTrack(songId)?.length ?: 0

    private companion object {
        const val ON = "1"
        const val OFF = "0"
        const val UNSEEN_FURNI = 1
        const val EDITOR_OPENED = -1
    }
}
