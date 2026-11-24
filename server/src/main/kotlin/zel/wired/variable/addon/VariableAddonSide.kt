package zel.wired.variable.addon

import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomRegistry
import zel.room.entry.DoorCheck
import zel.room.entry.EntryViews
import zel.room.furni.FurniBehaviour
import zel.room.furni.RoomFurni
import zel.wired.box.BoxKind
import zel.wired.box.BoxSettings
import zel.wired.box.BoxType
import zel.wired.box.WiredBox
import zel.wired.form.BoxValidation
import zel.wired.form.DialogContexts
import zel.wired.form.DialogSpecs
import zel.wired.form.WiredMasks
import zel.wired.protocol.BoxUse
import zel.wired.variable.VariableAddons
import zel.wired.variable.VariableChange
import zel.wired.variable.VariableOn

class VariableAddonSide(
    needs: AddonNeeds,
) {
    private val forms = needs.forms
    private val rows = needs.rows
    private val variables = needs.variables
    private val poster = needs.poster
    private val persist = needs.persist
    private val writer = needs.writer
    private val secret = needs.secret
    private val dataSource = needs.dataSource
    private val copies = ImportCopies(rows::extras)
    private val stack = StackAddons(forms, copies, needs.timezone)
    private val keyStore = JdbcWebApiKeys(needs.dataSource)
    private val webApi = WebApiBoxes(keyStore, needs.secret)
    private val apiNames = ApiVariables(variables)

    val fx = VariableFx(forms, stack, copies, variables, poster, needs.team, needs.codeOfBase, needs.roomLog)

    val addons: VariableAddons get() = stack

    val changeListeners: List<(VariableOn, VariableChange) -> Unit> = listOf(fx::changed)

    val validations: Map<String, BoxValidation> = VariableAddonRules(variables, fx, webApi).validations

    val parts: List<RoomPart> = listOf(apiNames, fx)

    val amounts = VariableAmounts(variables)

    val types: List<BoxType<WiredBox>> =
        (
            listOf(
                AddonCodes.TEXT_CONNECTOR,
                AddonCodes.TIME_UTIL,
                AddonCodes.LEVEL_UP,
                AddonCodes.SHARE,
                AddonCodes.IMPORT,
                AddonCodes.WEB_API,
            ) + FxKind.codes
        ).map { type(it, ::PassiveAddon) } +
            type(AddonCodes.TEXT_OUTPUT) { id, s -> TextOutputVariable(id, s, variables()) } +
            type(AddonCodes.TEXT_INPUT) { id, s -> TextInputVariable(id, s, variables()) }

    private fun type(
        code: String,
        create: (Int, BoxSettings) -> WiredBox,
    ): BoxType<WiredBox> = DialogSpecs.boxType(code, BoxKind.Addon, create)

    fun behaviours(use: BoxUse): List<Pair<String, FurniBehaviour>> {
        val ofFx = AddonFurni(use, fx::placed, fx::pickedUp)
        return FxKind.codes.map { it to ofFx } +
            (AddonCodes.IMPORT to AddonFurni(use, { _, _ -> }, fx::pickedUp)) +
            (AddonCodes.WEB_API to AddonFurni(use, { _, _ -> }, { _, box -> apiBoxGone(box) }))
    }

    private fun apiBoxGone(box: RoomFurni) {
        persist(Runnable { webApi.pickedUp(box) })
    }

    fun contexts(next: DialogContexts): DialogContexts = AddonContexts(variables, webApi, next)

    fun entryViews(next: EntryViews): EntryViews = FxEntryViews(fx, next)

    fun packets(
        registry: RoomRegistry,
        masks: (Room) -> WiredMasks,
        levels: (Room) -> Map<Int, Int>,
        hasRights: (Int, Int) -> Boolean,
    ): VariableAddonPackets {
        val sync =
            ShareSync(
                forms,
                rows,
                stack,
                copies,
                fx,
                variables,
                JdbcImportStore(dataSource),
                persist,
                writer,
                poster,
                registry::isLoaded,
                masks,
            )
        val mayModify = { room: Room, playerId: Int ->
            hasRights(room.info.id, playerId) && masks(room).mayModify(DoorCheck.RIGHTS)
        }
        val ops = BridgeOps(variables, apiNames, forms, mayModify)
        return VariableAddonPackets(registry, sync, webApi, levels) { VariablesBridge(secret, registry, ops) }
    }
}
