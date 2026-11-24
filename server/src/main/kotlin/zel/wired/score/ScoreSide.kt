package zel.wired.score

import zel.protocol.generated.VariableLeaderboardSubscribeInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.Room
import zel.room.RoomListener
import zel.room.RoomPart
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.entry.EntryViews
import zel.room.furni.FurniBehaviour
import zel.room.games.GamesPart
import zel.room.unit.RoomUnit
import zel.wired.BoxSettingsSource
import zel.wired.box.BoxKind
import zel.wired.box.BoxSettings
import zel.wired.box.BoxType
import zel.wired.box.WiredBox
import zel.wired.form.BoxValidation
import zel.wired.form.DialogContexts
import zel.wired.form.DialogSpecs
import zel.wired.form.SaveOutcome
import zel.wired.form.SaveRequest
import zel.wired.store.WiredStore
import zel.wired.store.WiredWriter
import zel.wired.timing.RoomPoster
import zel.wired.variable.RoomVariables
import javax.sql.DataSource

class ScoreSide(
    dataSource: DataSource,
    persist: (Runnable) -> Unit,
    writer: WiredWriter,
    levelOf: (Room) -> Map<Int, Int>,
    private val variables: () -> RoomVariables?,
    forms: () -> BoxSettingsSource?,
    private val poster: RoomPoster,
    games: () -> GamesPart?,
) {
    val highscores =
        Highscores(JdbcHighscoreStore(dataSource), writer, { room, player -> levelOf(room)[player] })
    val leaderboards = Leaderboards(RecentHolders(JdbcLeaderboardStore(dataSource)), variables, forms, persist, writer)

    val parts: List<RoomPart> get() = listOf(leaderboards, highscores)

    private val blobs = Blobs(games)

    val listener: RoomListener
        get() =
            RoomListener { scope, event ->
                leaderboards.on(scope, event)
                blobs.on(scope, event)
            }

    val behaviours: List<Pair<String, FurniBehaviour>>
        get() = listOf(Highscores.CODE to highscores, Blobs.CODE to blobs)

    val types: List<BoxType<WiredBox>> =
        listOf(
            BoxType(Leaderboards.CODE, BoxKind.Addon, DialogSpecs.TABLE.getValue(Leaderboards.CODE).form) { id, s ->
                LeaderboardAddon(id, s)
            },
        )

    fun validations(): Map<String, BoxValidation> =
        mapOf(
            Leaderboards.CODE to
                object : BoxValidation {
                    override fun check(
                        room: Room,
                        settings: BoxSettings,
                    ): SaveOutcome = SaveOutcome.Saved(settings)

                    override fun check(
                        room: Room,
                        settings: BoxSettings,
                        request: SaveRequest,
                    ): SaveOutcome = publishing(room, settings, request)
                },
        )

    /** The room sees the board right after the save's own answer goes out. */
    private fun publishing(
        room: Room,
        settings: BoxSettings,
        request: SaveRequest,
    ): SaveOutcome {
        val all = variables() ?: return SaveOutcome.Saved(settings)
        val rules = LeaderboardRules(all, leaderboards::atLimit, leaderboards::choiceCount)
        val outcome = rules.check(room, settings, request)
        val box = room.furni[request.boxId]
        if (outcome is SaveOutcome.Saved && box != null) {
            leaderboards.publish(room, box)
            poster.post(room.info.id, 0) { scope -> leaderboards.showPublished(scope) }
        }
        return outcome
    }

    fun contexts(next: DialogContexts): DialogContexts = LeaderboardContexts(leaderboards, variables, next)

    fun keeping(next: WiredStore): WiredStore = leaderboards.keeping(next)

    fun entryViews(next: EntryViews): EntryViews =
        object : EntryViews by next {
            override fun afterBurst(
                scope: RoomScope,
                unit: RoomUnit,
                playerId: Int,
            ) {
                next.afterBurst(scope, unit, playerId)
                leaderboards.entered(scope, playerId)
            }
        }

    fun packets(registry: RoomRegistry): PacketPackage =
        object : PacketPackage {
            override val name = "wired-leaderboards"
            override val routes: List<PacketRoute<*>> =
                listOf(
                    PacketRoute(VariableLeaderboardSubscribeInCodec) { session, m ->
                        subscribe(registry, session, SubscribeRequest(m.int1, m.int2, m.key, m.ids))
                    },
                )
        }

    private fun subscribe(
        registry: RoomRegistry,
        session: PacketSession,
        request: SubscribeRequest,
    ) {
        val playerId =
            session.client.player
                ?.profile
                ?.id ?: return
        val roomId = registry.roomOf(playerId) ?: return
        registry.withRoom(roomId) { scope -> leaderboards.subscribe(scope, session, playerId, request) }
    }
}
