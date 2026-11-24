package zel.wired.protocol

import zel.protocol.generated.ErrorReportOut
import zel.protocol.generated.ErrorReportOutCodec
import zel.protocol.generated.ObjectDataUpdateOut
import zel.protocol.generated.ObjectDataUpdateOutCodec
import zel.protocol.generated.WiredSaveSuccessOut
import zel.protocol.generated.WiredSaveSuccessOutCodec
import zel.protocol.generated.WiredValidationErrorOut
import zel.protocol.generated.WiredValidationErrorOutCodec
import zel.room.RoomScope
import zel.room.chat.ChatBubble
import zel.room.chat.ChatKind
import zel.room.chat.ChatOut
import zel.room.furni.RoomFurni
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Interactions
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.box.BoxCodes
import zel.wired.box.BoxForm
import zel.wired.box.BoxKind
import zel.wired.firing.FlashOut
import zel.wired.form.BoxValidation
import zel.wired.form.DialogSpecs
import zel.wired.form.SaveChecks
import zel.wired.form.SaveOutcome
import zel.wired.form.SaveRequest
import zel.wired.store.RoomBoxForms
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

class WiredForms(
    private val forms: RoomBoxForms,
    private val dialogs: DialogSpecs,
    private val out: DialogOut,
    private val validations: Map<String, BoxValidation> = emptyMap(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val afterSave: (RoomScope, RoomFurni) -> Unit = { _, _ -> },
    private val selectionLimit: () -> Int = { BoxForm.FURNI_LIMIT },
) {
    fun open(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        boxId: Int,
    ) {
        val box = scope.room.furni[boxId] ?: return
        val spec = dialogs[Interactions.of(box.definition)] ?: return
        if (!forms.masks(scope.room).mayRead(level)) return
        if (refusedAsOthersBox(scope, playerId, box)) return
        out.send(scope, playerId, box, spec, forms.settings(scope.room, box) ?: spec.form.fresh)
    }

    fun save(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        kind: BoxKind,
        request: SaveRequest,
    ) {
        val box = scope.room.furni[request.boxId]
        if (box == null) {
            if (forms.masks(scope.room).mayModify(level) && (kind == BoxKind.Effect || kind == BoxKind.Addon)) {
                invalid(scope, playerId, SaveOutcome.Invalid(notFound(request.boxId)))
            }
            return
        }
        val code = Interactions.of(box.definition)
        val spec = dialogs[code]?.takeIf { it.kind == kind } ?: return
        if (!forms.masks(scope.room).mayModify(level)) return
        if (refusedAsOthersBox(scope, playerId, box)) return
        val saved = request.copy(saverId = playerId)
        val validation = validations[code] ?: BoxValidation.None
        when (val outcome = SaveChecks.check(scope.room, spec, saved, validation, selectionLimit())) {
            is SaveOutcome.Saved -> {
                saved(scope, playerId, box, kind, outcome)
            }

            is SaveOutcome.Invalid -> {
                invalid(scope, playerId, outcome)
            }

            SaveOutcome.Ignored -> {}

            SaveOutcome.Report -> {
                report(
                    scope,
                    playerId,
                    if (code ==
                        BoxCodes.SCAN_CHEST
                    ) {
                        BoxKind.Selector.saveMessageId
                    } else {
                        kind.saveMessageId
                    },
                )
            }
        }
    }

    private fun notFound(boxId: Int) = "Wired effect/extra with item id $boxId not found in room"

    fun report(
        scope: RoomScope,
        playerId: Int,
        messageId: Int,
    ) {
        val time = STAMP.format(Instant.ofEpochMilli(clock()))
        scope.out.send(Lane.Now, to(playerId), ErrorReportOutCodec, ErrorReportOut(messageId, ERROR_CODE, time))
    }

    private fun refusedAsOthersBox(
        scope: RoomScope,
        playerId: Int,
        box: RoomFurni,
    ): Boolean {
        val refused = Interactions.of(box.definition) == Interactions.WEB_API && box.ownerId != playerId
        if (refused) invalid(scope, playerId, SaveOutcome.Invalid(OWNER_ONLY))
        return refused
    }

    private fun invalid(
        scope: RoomScope,
        playerId: Int,
        outcome: SaveOutcome.Invalid,
    ) {
        outcome.whisper?.let { whisper(scope, playerId, it) }
        val params = outcome.params.map { WiredValidationErrorOut.List1Item(it.first, it.second) }
        val error = WiredValidationErrorOut(outcome.key, params)
        scope.out.send(Lane.Now, to(playerId), WiredValidationErrorOutCodec, error)
    }

    private fun whisper(
        scope: RoomScope,
        playerId: Int,
        text: String,
    ) {
        val unit = scope.room.units.ofPlayer(playerId) ?: return
        val bubble = ChatBubble(unit.index, text, 0, NOTICE_STYLE, ChatBubble.NO_TRACKING, unit.index)
        ChatOut.send(scope, ChatKind.Whisper, playerId, bubble)
    }

    private fun saved(
        scope: RoomScope,
        playerId: Int,
        box: RoomFurni,
        kind: BoxKind,
        outcome: SaveOutcome.Saved,
    ) {
        forms.save(scope.room, box, outcome.settings)
        if (showsSelectorRest(box, kind)) {
            box.state = if (outcome.settings.filter) FlashOut.FILTER_REST else FlashOut.SELECTOR_REST
            scope.out.send(
                Lane.Now,
                Audience.Room,
                ObjectDataUpdateOutCodec,
                ObjectDataUpdateOut(box.id.toString(), FurniWire.stuff(box)),
            )
        }
        scope.out.send(Lane.Now, to(playerId), WiredSaveSuccessOutCodec, WiredSaveSuccessOut)
        afterSave(scope, box)
    }

    private fun showsSelectorRest(
        box: RoomFurni,
        kind: BoxKind,
    ): Boolean {
        val code = Interactions.of(box.definition)
        return (kind == BoxKind.Selector || code == BoxCodes.SCAN_CHEST) && code !in KEEPS_STATE
    }

    private fun to(playerId: Int): Audience = Audience.Player(playerId)

    private companion object {
        const val ERROR_CODE = 1
        const val NOTICE_STYLE = 1
        const val OWNER_ONLY = "wiredfurni.error.owner_only"

        val KEEPS_STATE = setOf("wf_slc_furni_with_var", "wf_slc_users_with_var")
        val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d/yy, h:mm a", Locale.US).withZone(ZoneOffset.UTC)
    }
}
