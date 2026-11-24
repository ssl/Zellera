package zel.room

import zel.protocol.typed.PacketPackage
import zel.room.entry.EntryViews
import zel.room.furni.FurniBehaviour
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.room.games.GamesPart
import zel.room.map.Point
import zel.wired.addon.AchievementEnabler
import zel.wired.addon.AchievementEnablerFurni
import zel.wired.addon.AchievementEnvironment
import zel.wired.addon.AddonRules
import zel.wired.addon.PlaceholderText
import zel.wired.addon.label.LabelArea
import zel.wired.box.BoxCatalog
import zel.wired.box.BoxForm
import zel.wired.box.pattern.PatternBoxes
import zel.wired.condition.ConditionBoxes
import zel.wired.condition.GameUnitFacts
import zel.wired.condition.WiredClock
import zel.wired.effect.furni.ApplySnapshots
import zel.wired.effect.furni.FurniEffectRules
import zel.wired.form.DialogContexts
import zel.wired.form.DialogSpecs
import zel.wired.link.FrameSaves
import zel.wired.protocol.BoxUse
import zel.wired.protocol.DialogOut
import zel.wired.protocol.RoomReloads
import zel.wired.protocol.WiredFormPackets
import zel.wired.protocol.WiredForms
import zel.wired.protocol.WiredMaskMenu
import zel.wired.selector.SelectorBoxes
import zel.wired.signal.LinkMessages
import zel.wired.signal.SignalBoxes
import zel.wired.store.JdbcWiredStore
import zel.wired.store.RoomBoxForms
import zel.wired.trigger.TriggerBoxes
import zel.wired.trigger.TriggerPackets
import zel.wired.variable.RoomVariables

internal class WiredBoxWiring(
    private val services: RoomEngineServices,
    private val core: CoreWiring,
    games: GamesPart,
    private val wired: WiredEngineWiring,
    val effects: WiredEffectWiring,
) {
    private val triggers =
        TriggerBoxes(
            playerName = core::playerName,
            botName = { scope, index ->
                core.units.bots
                    .byIndex(scope, index)
                    ?.bot
                    ?.name
            },
        )
    val recent = core.recent
    private val conditions =
        ConditionBoxes(
            games,
            wired.elapsed,
            WiredClock({ forms.masks(it).timezone }),
            GameUnitFacts(
                core.life,
                core.dances,
                core.units.bots,
                core.units.pets,
                services.clients::onlinePlayer,
            ) { scope, unit ->
                core.walking.stepOrigin(scope, unit)?.tile
            },
            recent,
            wired.chests.stock,
            wired.moveProbe,
            wired.chestAmounts,
        )
    private val validations =
        triggers.validations +
            FurniEffectRules.validations +
            SelectorBoxes.validations +
            AddonRules(effects::superWired).validations +
            FrameSaves.validations +
            SignalBoxes.validations +
            conditions.validations +
            effects.validations +
            wired.variables.validations +
            wired.addons.validations +
            wired.chestBoxes.validations +
            wired.scores.validations()
    val catalog =
        BoxCatalog(PatternBoxes.types + wired.types + triggers.types + conditions.types + effects.types)
    private val dialogs = DialogSpecs(catalog, DialogSpecs.TABLE + wired.chests.dialogs)
    val forms: RoomBoxForms =
        RoomBoxForms(
            wired.scores.keeping(JdbcWiredStore(services.dataSource)),
            dialogs,
            core.wiredWriter,
        ) { box -> Interactions.of(box.definition) in SHARED_SOURCES }
    val levels: (Room) -> Map<Int, Int> = core::levels
    private val use = BoxUse { room, playerId -> levels(room)[playerId] }
    private val environment = AchievementEnvironment(forms)
    val labels = LabelArea(services.dataSource, core.wiredWriter, forms, levels, effects::filteredText)

    val behaviours: List<Pair<String, FurniBehaviour>> = boxBehaviours() + labels.behaviours

    fun packets(
        registry: RoomRegistry,
        views: EntryViews,
        hasRights: (roomId: Int, playerId: Int) -> Boolean,
    ): List<PacketPackage> {
        val masks = forms::masks
        return listOf(
            formPackets(registry, views),
            labels.packets(registry),
            wired.linkPackets(registry),
            wired.monitor(registry, this),
            TriggerPackets(registry, forms, core.walking),
            wired.variables.packets(registry, masks, levels),
            wired.addons.packets(registry, masks, levels, hasRights),
            wired.chests.packets(registry, masks),
            wired.scores.packets(registry),
        )
    }

    private fun boxBehaviours(): List<Pair<String, FurniBehaviour>> {
        val variableBoxes =
            wired.variables.behaviours(use) +
                wired.addons.behaviours(use) +
                (AchievementEnabler.CODE to AchievementEnablerFurni(use, environment))
        val chestFurni = wired.chests.behaviours(use) + wired.scores.behaviours
        val own = (variableBoxes.map { it.first } + chestFurni.map { it.first }).toSet()
        return use.entries(dialogs.codes() - own) + variableBoxes + chestFurni + (LinkMessages.LINK to wired.linkFurni)
    }

    private fun formPackets(
        registry: RoomRegistry,
        views: EntryViews,
    ): WiredFormPackets =
        WiredFormPackets(
            registry,
            WiredForms(
                forms,
                dialogs,
                DialogOut(contexts(), ::selectionLimit),
                validations,
                afterSave = ::saved,
                selectionLimit = ::selectionLimit,
            ),
            WiredMaskMenu(forms, views),
            levels,
            RoomReloads(registry, core.leaving, wired.reloadPoints),
            ApplySnapshots(forms, wired.wired, wired.furniEffects::matchBox),
            { services.config.getInt(WIRED_MAX_DELAY_KEY, WiredFormPackets.DEFAULT_MAX_DELAY) },
        )

    private fun contexts(): DialogContexts =
        wired.variables.contexts(
            wired.addons.contexts(
                wired.scores.contexts(DialogContexts.Plain(wired.variables::hash, ::outputNames)),
            ),
        )

    private fun outputNames(
        room: Room,
        tile: Point,
    ): List<String> = PlaceholderText.namesOn(room, tile, forms)

    private fun saved(
        scope: RoomScope,
        box: RoomFurni,
    ) {
        if (Interactions.of(box.definition) == AchievementEnabler.CODE) environment.resend(scope)
    }

    private fun selectionLimit(): Int = services.config.getInt(SELECTION_KEY, BoxForm.FURNI_LIMIT)

    private companion object {
        const val WIRED_MAX_DELAY_KEY = "wired.box.max_delay.half_seconds"
        const val SELECTION_KEY = "wired.box.max_picked_furni"

        /** Variable boxes the owner's other rooms list as shared variables, read from their rows. */
        val SHARED_SOURCES = setOf(RoomVariables.USER_BOX, RoomVariables.ROOM_BOX)
    }
}
