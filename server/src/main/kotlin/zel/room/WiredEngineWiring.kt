package zel.room

import com.google.gson.JsonObject
import zel.core.config.GameEnvironment
import zel.core.rules.rules
import zel.game.players.level.AccountLevelTable
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.state.InvisibleFurni
import zel.room.furni.behaviour.state.StatePart
import zel.room.furni.place.Interactions
import zel.wired.StackBuilder
import zel.wired.StackRun
import zel.wired.WiredRooms
import zel.wired.addon.AddonBoxes
import zel.wired.addon.LinkSpeakers
import zel.wired.addon.UnitNames
import zel.wired.addon.UserCarrier
import zel.wired.box.BoxCatalog
import zel.wired.chest.ChestServices
import zel.wired.chest.ChestSide
import zel.wired.condition.ChestAmounts
import zel.wired.condition.MoveProbe
import zel.wired.effect.furni.FurniEffectBoxes
import zel.wired.effect.user.UserEffectState
import zel.wired.firing.FlashOut
import zel.wired.link.JdbcLinkStore
import zel.wired.link.LinkDialog
import zel.wired.link.LinkFurni
import zel.wired.link.LinkPackets
import zel.wired.score.ScoreSide
import zel.wired.selector.SelectorBoxes
import zel.wired.signal.JdbcLinkNetworks
import zel.wired.signal.LinkMessages
import zel.wired.signal.SignalBoxes
import zel.wired.signal.SignalRouter
import zel.wired.store.ReloadPoints
import zel.wired.store.RoomBoxForms
import zel.wired.store.RoomLog
import zel.wired.timing.DelayQueue
import zel.wired.timing.RoomPoster
import zel.wired.timing.TimerBoxes
import zel.wired.timing.WiredTimers
import zel.wired.trigger.ClockCounters
import zel.wired.usage.RunBudget
import zel.wired.usage.UsageBoxes
import zel.wired.usage.UsageGate
import zel.wired.usage.UsageMonitor
import zel.wired.usage.UsagePackets
import zel.wired.variable.GameValueTexts
import zel.wired.variable.GameVariableFacts
import zel.wired.variable.JdbcVariableStore
import zel.wired.variable.VariableFrames
import zel.wired.variable.VariableSide
import zel.wired.variable.addon.AddonNeeds
import zel.wired.variable.addon.ImportRows
import zel.wired.variable.addon.VariableAddonSide
import java.util.concurrent.atomic.AtomicReference

internal class WiredEngineWiring(
    private val services: RoomEngineServices,
    core: CoreWiring,
    private val state: StatePart,
) {
    private val walking = core.walking
    private val engine = AtomicReference<WiredRooms>()
    val wired: () -> WiredRooms? = { engine.get() }
    val poster = RoomPoster { roomId, delay, block -> core.rooms.get()?.post(roomId, delay, block) == true }
    private val boxForms = AtomicReference<RoomBoxForms>()
    val userEffects = AtomicReference<UserEffectState>()
    val reloadPoints = ReloadPoints(boxForms::get)

    fun roomLog(
        room: Room,
        line: RoomLog.Line,
    ) {
        boxForms.get()?.log(room)?.add(line)
    }

    private val usage: UsageGate =
        UsageGate(
            { AccountLevelTable.levelOf(it, services.clients.onlinePlayer(it), services.leaderboard()) },
            roomLog = ::roomLog,
            fixedLimit = { services.config.getInt(WIRED_USAGE_LIMIT_KEY, 0) },
            runLimits = ::runLimits,
            delayedCap = ::delayedCap,
        )
    private val delays = DelayQueue(wired, poster, usage, ::delayedCap)
    private val timers = WiredTimers(wired, poster)
    private val networks = JdbcLinkNetworks(services.dataSource, services.persist)
    private val linkDialog =
        LinkDialog(
            JdbcLinkStore(services.dataSource),
            networks::forget,
            maxNetworks = { services.config.getInt(LINK_NETWORKS_KEY, LinkDialog.DEFAULT_MAX_NETWORKS) },
        )
    val linkFurni = LinkFurni(linkDialog, services.persist)
    private val speakers = LinkSpeakers { room, furni -> boxForms.get()?.settings(room, furni) }
    private val messages = LinkMessages(wired, poster, networks, speakers::heard)
    private val signals = SignalBoxes(wired, SignalRouter(wired), messages, VariableFrames { variables.variables })
    val chests =
        ChestSide(
            ChestServices(
                services.dataSource,
                services.clients,
                services.config,
                services.persist,
                core.wiredWriter,
                services.items,
                services.wiredEarnings,
                services.progress,
                poster,
                ::chestText,
                services::progressLater,
            ),
            wired,
            boxForms::get,
            core::levels,
        )
    val scores =
        ScoreSide(
            services.dataSource,
            services.persist,
            core.wiredWriter,
            core::levels,
            { variables.variables },
            boxForms::get,
            poster,
            core.gameParts::get,
        )
    val furniEffects =
        FurniEffectBoxes(
            core.wiredUnitsOnTiles,
            poster,
            view = { InvisibleFurni.inView(state, it) },
            standsOn = { scope, unit -> core.walking.stepOrigin(scope, unit)?.tile ?: unit.tile },
        )
    val moveProbe: MoveProbe = furniEffects.moveProbe
    val chestAmounts =
        ChestAmounts { firing, box ->
            if (box.settings.intAt(CHEST_FROM_VARIABLE) == 0) {
                ChestAmounts.TYPED.amount(firing, box)
            } else {
                val target = box.settings.intAt(CHEST_VARIABLE_TARGET)
                variables.variables.readFor(firing, box, 0, target, furniSlot = 1, userSlot = 0)
            }
        }
    val addons: VariableAddonSide =
        VariableAddonSide(
            AddonNeeds(
                { room, box -> boxForms.get()?.settings(room, box) },
                object : ImportRows {
                    override fun extras(
                        room: Room,
                        box: RoomFurni,
                    ) = boxForms.get()?.extras(room, box)

                    override fun updateExtras(
                        room: Room,
                        box: RoomFurni,
                        change: (JsonObject) -> Unit,
                    ) {
                        boxForms.get()?.updateExtras(room, box, change)
                    }
                },
                { variables.variables },
                poster,
                services.persist,
                core.wiredWriter,
                services.dataSource,
                services.config.getValue(BRIDGE_SECRET_KEY, ""),
                ::timezone,
                { scope, playerId -> variables.facts.team(scope, playerId)?.color },
                { base -> services.definitions(base)?.let { Interactions.of(it) } },
                ::roomLog,
            ),
        )
    val variables: VariableSide =
        VariableSide(
            JdbcVariableStore(services.dataSource),
            { room, box -> boxForms.get()?.settings(room, box) },
            core.wiredWriter,
            wired,
            poster,
            core.wiredUnitsOnTiles,
            state,
            { spaces ->
                chests.facts(
                    GameVariableFacts(
                        core.leaving::doorOf,
                        core.life,
                        core.dances,
                        core.gameParts::get,
                        core.units.pets,
                        services.clients,
                        services.leaderboard,
                        spaces,
                        ::timezone,
                        core.transitStore,
                        { room, unit -> userEffects.get()?.isFrozen(room, unit) == true },
                        writer = core.wiredWriter,
                    ),
                )
            },
            GameValueTexts(
                { services.config.getValue(GameEnvironment.LOCAL_SITE_ROW, GameEnvironment.DEFAULT_LOCAL_SITE) },
                services.dataSource,
            ),
            addons.addons,
            addons.changeListeners + scores.leaderboards::heard,
            { usage.usage(it).allowance },
        )
    val chestBoxes = chests.boxes(variables.variables)
    val types =
        chestBoxes.types +
            scores.types +
            TimerBoxes(timers).types +
            signals.types +
            UsageBoxes.types +
            furniEffects.types +
            SelectorBoxes(wired, core.selectorFacts).types +
            AddonBoxes(
                UserCarrier(walking, poster),
                UnitNames(core.selectorFacts::playerName),
                addons.amounts::fromVariable,
                projectiles = variables.projectiles,
            ).types +
            variables.types +
            addons.types
    val elapsed: (RoomScope) -> Long = timers::elapsed

    fun engine(
        catalog: BoxCatalog,
        forms: RoomBoxForms,
    ): WiredParts {
        boxForms.set(forms)
        val run = StackRun(usage, delays)
        val built =
            WiredRooms(
                StackBuilder(catalog, forms),
                run,
                timers.tick,
                { rules.limits.wiredNesting },
                { runLimits().burstMillis },
                inView = { InvisibleFurni.inView(state, it) },
                blinkWindowMillis = FlashOut.BLINK_WINDOW_MILLIS,
            )
        engine.set(built)
        val own = listOf(built, delays, timers, usage, variables.part, reloadPoints)
        val parts = own + addons.parts + chests.parts + scores.parts
        return WiredParts(built, parts)
    }

    fun linkPackets(registry: RoomRegistry) = LinkPackets(registry, linkDialog)

    fun monitor(
        registry: RoomRegistry,
        boxes: WiredBoxWiring,
    ): UsagePackets {
        val monitor = UsageMonitor(usage, boxes.forms::masks, { rules.roomMaxFurni }, variables::permanentCounts)
        return UsagePackets(registry, monitor, boxes.levels)
    }

    fun clockCounted(
        scope: RoomScope,
        clockId: Int,
        millis: Int,
    ) = engine.get()?.let { ClockCounters(it).counted(scope, clockId, millis) } ?: Unit

    private fun timezone(room: Room): String =
        boxForms
            .get()
            ?.masks(room)
            ?.timezone
            .orEmpty()

    private fun chestText(
        playerId: Int,
        text: String,
    ): String = services.wordFilter()?.filter(text, services.clients.onlinePlayer(playerId)) ?: text

    private fun delayedCap(): Int = services.config.getInt(DELAYED_LIMIT_KEY, DelayQueue.MAX_WAITING)

    private fun runLimits(): RunBudget.Limits {
        val fresh = RunBudget.Limits()
        val config = services.config
        return RunBudget.Limits(
            config.getInt("wired.monitor.run_share.percent", fresh.sharePercent),
            config.getInt("wired.monitor.run_burst.ms", fresh.burstMillis),
            config.getInt("wired.monitor.heavy_after.seconds", fresh.heavyAfterSeconds),
            config.getInt("wired.monitor.pause_after.seconds", fresh.pauseAfterSeconds),
            config.getInt("wired.monitor.pause.seconds", fresh.pauseSeconds),
        )
    }

    class WiredParts(
        val engine: WiredRooms,
        val parts: List<RoomPart>,
    )

    private companion object {
        const val CHEST_FROM_VARIABLE = 1
        const val CHEST_VARIABLE_TARGET = 2
        const val WIRED_USAGE_LIMIT_KEY = "wired.monitor.usage_allowance"
        const val DELAYED_LIMIT_KEY = "wired.monitor.max_waiting_runs"
        const val LINK_NETWORKS_KEY = "wired.links.networks_per_player"
        const val BRIDGE_SECRET_KEY = "wired.variables.bridge.secret"
    }
}
