package zel.game.bots

import zel.db.allOrNothing
import zel.db.write
import zel.db.writeEach
import java.sql.Connection
import java.sql.SQLException

/** Bot chat lines: no rows speaks one empty default line, while a single empty row means no lines at all. */
object BotChatLines {
    const val COLUMN_NAME = "chat_lines"

    /** The rows of `bots.id` packed into one column, each line closed by a carriage return; NULL without rows. */
    const val COLUMN =
        "(SELECT GROUP_CONCAT(CONCAT(line, CHAR(13)) ORDER BY position SEPARATOR '') " +
            "FROM bot_chat_lines WHERE bot_id = bots.id) AS $COLUMN_NAME"

    private const val LINE_END = '\r'
    private const val CLEAR = "DELETE FROM bot_chat_lines WHERE bot_id = ?"
    private const val ADD = "INSERT INTO bot_chat_lines (bot_id, position, line) VALUES (?, ?, ?)"
    private const val COPY =
        "INSERT INTO bot_chat_lines (bot_id, position, line) " +
            "SELECT ?, position, line FROM bot_chat_lines WHERE bot_id = ?"

    fun unpack(packed: String?): List<String> = packed?.split(LINE_END)?.dropLastWhile(String::isEmpty) ?: listOf("")

    fun pack(lines: List<String>): List<String> =
        if (lines.isEmpty()) listOf("") else lines.dropLastWhile(String::isEmpty)

    /** [alongside] runs first, in the same transaction. */
    @Throws(SQLException::class)
    fun rewrite(
        connection: Connection,
        bot: Int,
        lines: List<String>,
        alongside: () -> Unit = {},
    ) {
        connection.allOrNothing {
            alongside()
            connection.write(CLEAR) { int(bot) }
            connection.writeEach(ADD, pack(lines).withIndex()) { (position, line) ->
                int(bot)
                int(position)
                string(line)
            }
        }
    }

    @Throws(SQLException::class)
    fun copyLines(
        connection: Connection,
        from: Int,
        to: Int,
    ) {
        connection.write(COPY) {
            int(to)
            int(from)
        }
    }
}
