package zel.wired.form

import zel.wired.box.BoxCatalog
import zel.wired.box.BoxForm
import zel.wired.box.BoxKind
import zel.wired.box.BoxSettings
import zel.wired.box.BoxType
import zel.wired.box.SourceSlot
import zel.wired.box.WiredBox

class DialogSpecs(
    catalog: BoxCatalog,
    table: Map<String, DialogSpec> = TABLE,
) {
    private val byCode: Map<String, DialogSpec> =
        table +
            catalog.codes().mapNotNull { code ->
                catalog[code]?.let { type -> code to merged(table[code], code, type.kind, type.form) }
            }

    operator fun get(code: String): DialogSpec? = byCode[code]

    fun codes(): Set<String> = byCode.keys

    private fun merged(
        row: DialogSpec?,
        code: String,
        kind: BoxKind,
        form: BoxForm,
    ): DialogSpec = row?.copy(kind = kind, form = form) ?: DialogSpec(code, kind, form)

    companion object {
        /** A box type whose form is the code's row in [TABLE]. */
        fun <B : WiredBox> boxType(
            code: String,
            kind: BoxKind,
            create: (Int, BoxSettings) -> B,
        ): BoxType<B> = BoxType(code, kind, TABLE.getValue(code).form, create)

        val TABLE: Map<String, DialogSpec> =
            listOf(
                DialogTable.TRIGGERS to BoxKind.Trigger,
                DialogTable.EFFECTS to BoxKind.Effect,
                DialogTable.CONDITIONS to BoxKind.Condition,
                DialogTable.SELECTORS to BoxKind.Selector,
                DialogTable.ADDONS to BoxKind.Addon,
                DialogTable.VARIABLES to BoxKind.Variable,
            ).flatMap { (text, kind) -> parse(text, kind) }
                .associateBy { it.code }

        fun parse(
            text: String,
            kind: BoxKind,
        ): List<DialogSpec> =
            text
                .lines()
                .filter { it.isNotBlank() }
                .fold(mutableListOf<String>()) { rows, line ->
                    if (line.startsWith(CONTINUED) && rows.isNotEmpty()) {
                        rows[rows.lastIndex] = rows.last() + " " + line.trim()
                    } else {
                        rows.add(line.trim())
                    }
                    rows
                }.map { row(it, kind) }

        private fun row(
            line: String,
            kind: BoxKind,
        ): DialogSpec {
            val tokens = line.split(' ')
            val code = tokens[0]
            val fields = tokens.drop(2)

            fun values(key: String): List<String> =
                fields.filter { it.startsWith("$key=") }.map { it.substringAfter('=') }

            fun ints(key: String): List<Int>? = values(key).firstOrNull()?.let(::intList)
            val furni = values("f").flatMap(::slots)
            val users = values("u").flatMap(::slots)
            val freshInts = ints("i").orEmpty()
            val fresh =
                BoxSettings.EMPTY.copy(
                    ints = freshInts,
                    furniSources = ints("fs") ?: furni.map { it.fresh },
                    userSources = users.map { it.fresh },
                    variableIds = values("v").firstOrNull()?.split(',').orEmpty(),
                )
            val form =
                BoxForm(
                    dialogCode = tokens[1].toInt(),
                    fresh = fresh,
                    furniSlots = furni,
                    userSlots = users,
                    allowWallItems = "wall" in fields,
                    furniLimit = ints("limit")?.single(),
                    textLimit = ints("text")?.single() ?: BoxForm.TEXT_LIMIT,
                )
            return DialogSpec(
                code = code,
                kind = kind,
                form = form,
                advanced = "basic" !in fields,
                quantifierType = ints("q")?.single() ?: 0,
                invert = "invert" in fields,
                contexts = ints("ctx").orEmpty(),
                closingInts = ints("d") ?: freshInts,
            )
        }

        private fun slots(text: String): List<SourceSlot> =
            text.split(';').map { slot ->
                val (fresh, offered) = slot.split(':')
                SourceSlot(offered.split('/').map(String::toInt).toSet(), fresh.toInt())
            }

        private fun intList(text: String): List<Int> =
            if (text.isEmpty()) emptyList() else text.split(',').map(String::toInt)

        private const val CONTINUED = "          "
    }
}
