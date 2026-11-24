package zel.room.chat

sealed interface ChatKind {
    data object Talk : ChatKind

    data object Shout : ChatKind

    data object Whisper : ChatKind
}
