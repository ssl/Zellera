package zel.game.bots

import zel.db.rows
import java.sql.Connection
import java.sql.SQLException

/** Butler keywords: a row's keyword, trimmed and lower-case, maps to the row's hand item. */
@Throws(SQLException::class)
fun readButlerWords(connection: Connection): Map<String, Int> =
    connection
        .rows("SELECT keyword, handitem_id FROM bot_serve_keywords") { row ->
            row
                .getString("keyword")
                .orEmpty()
                .trim()
                .lowercase() to row.getInt("handitem_id")
        }.filter { (keyword, _) -> keyword.isNotEmpty() }
        .toMap()
