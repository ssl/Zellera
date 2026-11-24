package zel.game.items

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.Strictness
import java.nio.file.Path
import kotlin.io.path.extension

/** JSON with `//` and `/* */` comments and trailing commas; nothing else of JSON5 is read. */
object Jsonc {
    private val EXTENSIONS = setOf("json", "jsonc")
    private val TRAILING_COMMA = Regex(""",(\s*)(?=[\]}])""")
    private val STRICT = GsonBuilder().setStrictness(Strictness.STRICT).create()

    fun isSupported(file: Path): Boolean = file.extension.lowercase() in EXTENSIONS

    fun parse(text: String): JsonObject =
        STRICT.fromJson(plainJson(text), JsonElement::class.java) as? JsonObject
            ?: throw JsonParseException("the document is not a JSON object")

    private enum class Kind { QUOTED, COMMENT, CODE }

    private class Segment(
        val kind: Kind,
        val text: String,
    )

    private fun plainJson(text: String): String =
        segments(text)
            .filterNot { it.kind == Kind.COMMENT }
            .fold(emptyList<Segment>(), ::joinCode)
            .joinToString("") { if (it.kind == Kind.CODE) TRAILING_COMMA.replace(it.text, "$1") else it.text }

    /** Two code segments that only a comment kept apart read as one, so a comma and its closer meet. */
    private fun joinCode(
        joined: List<Segment>,
        next: Segment,
    ): List<Segment> {
        val last = joined.lastOrNull()
        return if (last != null && last.kind == Kind.CODE && next.kind == Kind.CODE) {
            joined.dropLast(1) + Segment(Kind.CODE, last.text + next.text)
        } else {
            joined + next
        }
    }

    private fun segments(text: String): List<Segment> {
        val found = ArrayList<Segment>()
        var cursor = 0
        while (cursor < text.length) {
            val (kind, end) =
                when {
                    text.startsWith("//", cursor) -> Kind.COMMENT to lineCommentEnd(text, cursor)
                    text.startsWith("/*", cursor) -> Kind.COMMENT to blockCommentEnd(text, cursor)
                    text[cursor] == '"' -> Kind.QUOTED to quotedEnd(text, cursor)
                    else -> Kind.CODE to codeEnd(text, cursor)
                }
            found += Segment(kind, text.substring(cursor, end))
            cursor = end
        }
        return found
    }

    private fun lineCommentEnd(
        text: String,
        from: Int,
    ): Int = text.indexOf('\n', from).takeIf { it >= 0 } ?: text.length

    private fun blockCommentEnd(
        text: String,
        from: Int,
    ): Int =
        text.indexOf("*/", from + 2).takeIf { it >= 0 }?.plus(2)
            ?: throw JsonParseException("a block comment is not closed")

    private fun quotedEnd(
        text: String,
        from: Int,
    ): Int {
        var at = from + 1
        while (at < text.length) {
            when (text[at]) {
                '\\' -> at += 2
                '"' -> return at + 1
                else -> at++
            }
        }
        return text.length
    }

    private fun codeEnd(
        text: String,
        from: Int,
    ): Int {
        val next =
            (from until text.length).firstOrNull { i ->
                text[i] == '"' || (text[i] == '/' && i + 1 < text.length && text[i + 1] in "/*")
            }
        return next ?: text.length
    }
}
