package zel.wired.signal

import zel.room.furni.place.Interactions
import zel.wired.WiredRooms
import zel.wired.box.BoxForm
import zel.wired.box.BoxKind
import zel.wired.box.BoxSettings
import zel.wired.box.BoxType
import zel.wired.box.SourceSlot
import zel.wired.box.WiredBox
import zel.wired.firing.FurniSource
import zel.wired.firing.UserSource
import zel.wired.form.BoxValidation
import zel.wired.form.DialogSpecs
import zel.wired.form.SaveOutcome
import kotlin.math.abs

class SignalBoxes(
    rooms: () -> WiredRooms?,
    router: SignalRouter,
    messages: LinkMessages,
    frameVariables: FrameVariables = FrameVariables.None,
) {
    /** Forms come from the dialog table, except send dataframe's, whose twelve rows the table does not spell out. */
    val types: List<BoxType<WiredBox>> =
        listOf(
            type("wf_act_call_stacks", BoxKind.Effect) { id, s -> CallStacks(id, s, negative = false, rooms) },
            type("wf_act_neg_call_stacks", BoxKind.Effect) { id, s -> CallStacks(id, s, negative = true, rooms) },
            type("wf_act_send_signal", BoxKind.Effect) { id, s -> SendSignal(id, s, negative = false, router) },
            type("wf_act_neg_send_signal", BoxKind.Effect) { id, s -> SendSignal(id, s, negative = true, router) },
            type("wf_trg_recv_signal", BoxKind.Trigger, ::ReceiveSignal),
            type("wf_act_send_message", BoxKind.Effect) { id, s -> SendMessage(id, s, messages) },
            BoxType("wf_act_send_dataframe", BoxKind.Effect, sendFrameForm()) { id, s ->
                SendDataframe(id, s, messages, frameVariables)
            },
            type("wf_trg_receive_message", BoxKind.Trigger, ::ReceiveMessage),
            type("wf_trg_receive_dataframe", BoxKind.Trigger, ::ReceiveDataframe),
        )

    private fun type(
        code: String,
        kind: BoxKind,
        create: (Int, BoxSettings) -> WiredBox,
    ): BoxType<WiredBox> = DialogSpecs.boxType(code, kind, create)

    private fun sendFrameForm(): BoxForm {
        val furni =
            setOf(
                FurniSource.TRIGGERING,
                FurniSource.PICKED,
                FurniSource.PICKED_SECOND,
                FurniSource.SELECTION,
                FurniSource.SIGNAL,
            )
        val users =
            SourceSlot(setOf(UserSource.TRIGGERING, UserSource.SELECTION, UserSource.SIGNAL), UserSource.TRIGGERING)
        val rows = SendDataframe.MAX_ROWS
        val furniSlots =
            listOf(SourceSlot(furni, FurniSource.PICKED)) + List(rows) { SourceSlot(furni, FurniSource.SELECTION) }
        return BoxForm(
            dialogCode = SEND_FRAME_CODE,
            fresh =
                BoxSettings.EMPTY.copy(
                    ints = listOf(0) + List(rows) { listOf(0, 0, FRESH_ROW_TARGET) }.flatten(),
                    furniSources = furniSlots.map { it.fresh },
                    userSources = List(rows) { UserSource.TRIGGERING },
                    variableIds = List(rows) { NO_VARIABLE },
                ),
            furniSlots = furniSlots,
            userSlots = List(rows) { users },
            allowWallItems = true,
            textLimit = SEND_FRAME_TEXT,
        )
    }

    companion object {
        val validations: Map<String, BoxValidation> =
            mapOf(
                "wf_act_send_signal" to antennasOnly(SEND_NOT_ANTENNA),
                "wf_act_neg_send_signal" to antennasOnly(SEND_NOT_ANTENNA),
                "wf_trg_recv_signal" to antennasOnly(RECEIVE_NOT_ANTENNA),
            )

        const val SEND_NOT_ANTENNA = "A signal can only be sent to antenna furni."
        const val RECEIVE_NOT_ANTENNA = "A signal can only be received from antenna furni."
        private const val ANTENNA = "wired.antenna"
        private const val SEND_FRAME_CODE = 64

        /** Twelve rows of a name and a message at their longest, as JSON. */
        private const val SEND_FRAME_TEXT = 6000
        private const val FRESH_ROW_TARGET = -20
        private const val NO_VARIABLE = "0"

        private fun antennasOnly(key: String) =
            BoxValidation { room, settings ->
                val wrong =
                    settings.picks.any { pick ->
                        room.furni[abs(pick)]?.let { Interactions.of(it.definition) != ANTENNA } == true
                    }
                if (wrong) SaveOutcome.Invalid(key) else SaveOutcome.Saved(settings)
            }
    }
}
