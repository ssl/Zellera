package zel.room.chat

sealed interface ChatFilterResult {
    data class Say(
        val text: String,
    ) : ChatFilterResult

    data object Hide : ChatFilterResult
}
