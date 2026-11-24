package zel.wired.chest

/** What the chest and contract rows share: the row id of a furni id, the text limit and the tiny columns. */
internal object ChestRows {
    const val ROW_ID = "COALESCE((SELECT furni_id FROM build_pass_furni WHERE client_id = ?), ?)"

    const val MAX_TEXT = 255

    fun tiny(value: Int): Int = value.coerceIn(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt())
}
