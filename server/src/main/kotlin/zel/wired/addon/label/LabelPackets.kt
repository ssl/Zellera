package zel.wired.addon.label

import zel.protocol.generated.SetFurnitureLabelInCodec
import zel.protocol.generated.SetFurnitureLabelSettingsInCodec
import zel.protocol.generated.WiredValidationErrorOut
import zel.protocol.generated.WiredValidationErrorOutCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.store.RoomBoxForms

class LabelPackets(
    private val registry: RoomRegistry,
    private val labels: RoomLabels,
    private val out: LabelOut,
    private val forms: RoomBoxForms,
    private val levels: (zel.room.Room) -> Map<Int, Int>,
    private val filterText: (Int, String) -> String,
) : PacketPackage {
    override val name = "wired-label"
    override val routes =
        listOf(
            PacketRoute(SetFurnitureLabelInCodec) { session, m ->
                edit(session, m.furniId) { scope, furni, playerId -> setText(scope, furni, playerId, m.text) }
            },
            PacketRoute(SetFurnitureLabelSettingsInCodec) { session, m ->
                edit(session, m.furniId) { scope, furni, playerId ->
                    setSettings(scope, furni, playerId, m.int1, m.int2, m.flag)
                }
            },
        )

    private fun edit(
        session: PacketSession,
        furniId: Int,
        change: (RoomScope, RoomFurni, Int) -> Unit,
    ) {
        val playerId =
            session.client.player
                ?.profile
                ?.id ?: return
        val roomId = registry.roomOf(playerId) ?: return
        registry.withRoom(roomId) { scope ->
            val furni = scope.room.furni[furniId]?.takeIf { Interactions.of(it.definition) == Label.CODE }
            if (furni != null && present(scope, playerId)) change(scope, furni, playerId)
        }
    }

    private fun setText(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        text: String,
    ) {
        if (!mayEdit(scope, playerId)) return refuse(scope, playerId, Label.NO_WRITE_RIGHTS)
        if (Label.ignored(text)) return
        if (Label.tooLong(text)) {
            return refuse(
                scope,
                playerId,
                Label.TOO_MUCH_TEXT,
                "limit" to Label.MAX_TEXT_BYTES.toString(),
            )
        }
        val shown = filterText(playerId, text)
        val label = labels.of(scope.room, furni.id)
        if (label.text != shown) apply(scope, furni, label.copy(text = shown))
    }

    private fun setSettings(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        color: Int,
        visibility: Int,
        alwaysOpen: Boolean,
    ) {
        if (!mayEdit(scope, playerId)) return refuse(scope, playerId, Label.NO_WRITE_RIGHTS)
        if (!Label.validSettings(color, visibility)) return refuse(scope, playerId, Label.INVALID_SETTINGS)
        val label = labels.of(scope.room, furni.id)
        val next = label.withSettings(color, visibility, alwaysOpen)
        if (next != label) apply(scope, furni, next)
    }

    private fun apply(
        scope: RoomScope,
        furni: RoomFurni,
        label: Label,
    ) {
        labels.set(scope.room, furni.id, label)
        out.changed(scope, furni.id, label, data = true)
    }

    private fun present(
        scope: RoomScope,
        playerId: Int,
    ): Boolean = playerId in levels(scope.room)

    private fun mayEdit(
        scope: RoomScope,
        playerId: Int,
    ): Boolean = levels(scope.room)[playerId]?.let { forms.masks(scope.room).mayModify(it) } == true

    private fun refuse(
        scope: RoomScope,
        playerId: Int,
        key: String,
        vararg params: Pair<String, String>,
    ) {
        val error =
            WiredValidationErrorOut(key, params.map { WiredValidationErrorOut.List1Item(it.first, it.second) })
        scope.out.send(Lane.Now, Audience.Player(playerId), WiredValidationErrorOutCodec, error)
    }
}
