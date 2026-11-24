package zel.wired.protocol

import zel.protocol.generated.PacketWriter
import zel.protocol.generated.WiredFurniActionOut
import zel.protocol.generated.WiredFurniActionOutCodec
import zel.protocol.generated.WiredFurniAddonOut
import zel.protocol.generated.WiredFurniAddonOutCodec
import zel.protocol.generated.WiredFurniConditionOut
import zel.protocol.generated.WiredFurniConditionOutCodec
import zel.protocol.generated.WiredFurniSelectorOut
import zel.protocol.generated.WiredFurniSelectorOutCodec
import zel.protocol.generated.WiredFurniTriggerOut
import zel.protocol.generated.WiredFurniTriggerOutCodec
import zel.protocol.generated.WiredFurniVariableOut
import zel.protocol.generated.WiredFurniVariableOutCodec
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.box.BoxForm
import zel.wired.box.BoxKind
import zel.wired.box.BoxSettings
import zel.wired.form.DialogContexts
import zel.wired.form.DialogSpec
import zel.wired.form.SourceOffers
import kotlin.math.abs

class DialogOut(
    private val contexts: DialogContexts,
    private val selectionLimit: () -> Int = { BoxForm.FURNI_LIMIT },
) {
    @Suppress("LongMethod") // one constructor call per dialog kind, all from the same fields
    fun send(
        scope: RoomScope,
        playerId: Int,
        box: RoomFurni,
        spec: DialogSpec,
        settings: BoxSettings,
    ) {
        val d = fields(scope, box, spec, settings)
        val to = Audience.Player(playerId)
        val out = scope.out
        when (spec.kind) {
            BoxKind.Trigger -> {
                out.send(
                    Lane.Now,
                    to,
                    WiredFurniTriggerOutCodec,
                    WiredFurniTriggerOut(
                        d.limit,
                        d.picks,
                        d.picks2,
                        d.type,
                        d.id,
                        d.text,
                        d.ints,
                        d.vars,
                        d.furni,
                        d.users,
                        d.code,
                        d.advanced,
                        d.allowedFurni.map { WiredFurniTriggerOut.List7Item(it) },
                        d.allowedUsers.map { WiredFurniTriggerOut.List8Item(it) },
                        d.freshFurni,
                        d.freshUsers,
                        d.wall,
                        d.tail,
                    ),
                )
            }

            BoxKind.Effect -> {
                out.send(
                    Lane.Now,
                    to,
                    WiredFurniActionOutCodec,
                    WiredFurniActionOut(
                        d.limit,
                        d.picks,
                        d.picks2,
                        d.type,
                        d.id,
                        d.text,
                        d.ints,
                        d.vars,
                        d.furni,
                        d.users,
                        d.code,
                        settings.delay,
                        d.advanced,
                        d.allowedFurni.map { WiredFurniActionOut.List7Item(it) },
                        d.allowedUsers.map { WiredFurniActionOut.List8Item(it) },
                        d.freshFurni,
                        d.freshUsers,
                        d.wall,
                        d.tail,
                    ),
                )
            }

            BoxKind.Condition -> {
                out.send(
                    Lane.Now,
                    to,
                    WiredFurniConditionOutCodec,
                    WiredFurniConditionOut(
                        d.limit,
                        d.picks,
                        d.picks2,
                        d.type,
                        d.id,
                        d.text,
                        d.ints,
                        d.vars,
                        d.furni,
                        d.users,
                        d.code,
                        settings.quantifier,
                        d.advanced,
                        d.allowedFurni.map { WiredFurniConditionOut.List7Item(it) },
                        d.allowedUsers.map { WiredFurniConditionOut.List8Item(it) },
                        d.freshFurni,
                        d.freshUsers,
                        d.wall,
                        spec.quantifierType.toByte(),
                        spec.invert,
                        d.tail,
                    ),
                )
            }

            BoxKind.Selector -> {
                out.send(
                    Lane.Now,
                    to,
                    WiredFurniSelectorOutCodec,
                    WiredFurniSelectorOut(
                        d.limit,
                        d.picks,
                        d.picks2,
                        d.type,
                        d.id,
                        d.text,
                        d.ints,
                        d.vars,
                        d.furni,
                        d.users,
                        d.code,
                        settings.filter,
                        settings.invert,
                        d.advanced,
                        d.allowedFurni.map { WiredFurniSelectorOut.List7Item(it) },
                        d.allowedUsers.map { WiredFurniSelectorOut.List8Item(it) },
                        d.freshFurni,
                        d.freshUsers,
                        d.wall,
                        d.tail,
                    ),
                )
            }

            BoxKind.Addon -> {
                out.send(
                    Lane.Now,
                    to,
                    WiredFurniAddonOutCodec,
                    WiredFurniAddonOut(
                        d.limit,
                        d.picks,
                        d.picks2,
                        d.type,
                        d.id,
                        d.text,
                        d.ints,
                        d.vars,
                        d.furni,
                        d.users,
                        d.code,
                        d.advanced,
                        d.allowedFurni.map { WiredFurniAddonOut.List7Item(it) },
                        d.allowedUsers.map { WiredFurniAddonOut.List8Item(it) },
                        d.freshFurni,
                        d.freshUsers,
                        d.wall,
                        d.tail,
                    ),
                )
            }

            BoxKind.Variable -> {
                out.send(
                    Lane.Now,
                    to,
                    WiredFurniVariableOutCodec,
                    WiredFurniVariableOut(
                        d.limit,
                        d.picks,
                        d.picks2,
                        d.type,
                        d.id,
                        d.text,
                        d.ints,
                        d.vars,
                        d.furni,
                        d.users,
                        d.code,
                        d.advanced,
                        d.allowedFurni.map { WiredFurniVariableOut.List7Item(it) },
                        d.allowedUsers.map { WiredFurniVariableOut.List8Item(it) },
                        d.freshFurni,
                        d.freshUsers,
                        d.wall,
                        d.tail,
                    ),
                )
            }
        }
    }

    private fun fields(
        scope: RoomScope,
        box: RoomFurni,
        spec: DialogSpec,
        settings: BoxSettings,
    ): Fields {
        val form = spec.form
        val tail = PacketWriter()
        val entries = PacketWriter()
        val count = contexts.write(scope.room, box, spec, entries)
        tail.int(count).bytes(entries.toByteArray())
        tail.int(spec.closingInts.size)
        spec.closingInts.forEach { tail.int(it) }
        return Fields(
            limit = form.furniLimit ?: selectionLimit(),
            picks = present(scope, settings.picks),
            picks2 = present(scope, settings.picks2),
            type = box.definition.spriteId,
            id = box.id,
            text = contexts.shownText(scope.room, box, settings),
            ints = settings.ints,
            vars = contexts.shownVariableIds(scope.room, box, settings),
            furni = settings.furniSources,
            users = settings.userSources,
            code = form.dialogCode,
            advanced = spec.advanced,
            allowedFurni = form.furniSlots.map { it.offered.toList() },
            allowedUsers = SourceOffers.userSlots(scope.room, form.userSlots).map { it.offered.toList() },
            freshFurni = form.furniSlots.map { it.fresh },
            freshUsers = form.userSlots.map { it.fresh },
            wall = form.allowWallItems,
            tail = tail.toByteArray(),
        )
    }

    /** In stored order (a save sorts them); a wall item is sent negative. */
    private fun present(
        scope: RoomScope,
        picks: List<Int>,
    ): List<Int> =
        picks
            .distinct()
            .filter { scope.room.furni[abs(it)] != null }

    private class Fields(
        val limit: Int,
        val picks: List<Int>,
        val picks2: List<Int>,
        val type: Int,
        val id: Int,
        val text: String,
        val ints: List<Int>,
        val vars: List<String>,
        val furni: List<Int>,
        val users: List<Int>,
        val code: Int,
        val advanced: Boolean,
        val allowedFurni: List<List<Int>>,
        val allowedUsers: List<List<Int>>,
        val freshFurni: List<Int>,
        val freshUsers: List<Int>,
        val wall: Boolean,
        val tail: ByteArray,
    )
}
