package zel.game.moderation

import zel.core.Configuration
import zel.db.Sql
import zel.db.orFallback
import zel.game.permissions.Privilege
import zel.game.players.Player
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class WordFilter(
    private val config: Configuration,
    dataSource: DataSource,
) {
    @Volatile
    private var words: List<FilteredWord> =
        if (config.getBoolean("word_filter.is_on")) read(dataSource) else emptyList()

    @Volatile
    private var list = listFor(words)

    @Synchronized
    fun add(word: FilteredWord) {
        words = words + word
        list = listFor(words)
    }

    fun hides(text: String): Boolean = list.hides(text)

    /** The text with every filtered word replaced; a word with a mute time mutes [player] too. */
    fun filter(
        text: String,
        player: Player?,
    ): String {
        if (player?.hasPermission(Privilege.CHAT_UNFILTERED) == true) return text
        val censored = list.censor(text)
        val mute = censored.found.maxOfOrNull { it.muteSeconds } ?: 0
        if (player != null && mute > 0) player.mute(mute)
        return censored.text
    }

    private fun listFor(words: List<FilteredWord>) =
        WordList(
            words,
            if (config.getBoolean("word_filter.normalises_text")) {
                ChatNormaliser::normalise
            } else {
                ChatNormaliser::lowercase
            },
        )

    private companion object {
        val log: Logger = LoggerFactory.getLogger(WordFilter::class.java)

        fun read(dataSource: DataSource): List<FilteredWord> =
            log.orFallback("word filter", emptyList()) {
                Sql(dataSource)
                    .select("SELECT word, replacement, mute_seconds FROM word_filters")
                    .rows { row ->
                        val word = row.getString("word").orEmpty()
                        FilteredWord(word, row.getString("replacement"), row.getInt("mute_seconds"))
                    }
            }
    }
}
