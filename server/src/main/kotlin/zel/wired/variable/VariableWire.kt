package zel.wired.variable

import zel.protocol.generated.PacketWriter

object VariableWire {
    fun block(
        out: PacketWriter,
        entry: VariableEntry,
    ) {
        out
            .string(entry.id)
            .int(entry.type)
            .string(entry.name)
            .int(entry.availability)
            .int(entry.target)
        entry.flags.all().forEach { out.boolean(it) }
        texts(out, entry.texts)
    }

    fun texts(
        out: PacketWriter,
        texts: ValueTexts?,
    ) {
        out.boolean(texts != null)
        if (texts != null) {
            out.int(texts.entries.size)
            texts.entries.forEach { (value, text) -> out.int(value).string(text) }
            out.boolean(texts.fallback != null)
            texts.fallback?.let { out.string(it) }
        }
    }

    fun shown(
        entry: VariableEntry,
        value: Long,
    ): Int = if (entry.flags.hasValue) shown(value) else FLAG_VALUE

    fun shown(value: Long): Int = value.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()

    private const val FLAG_VALUE = -1
}
