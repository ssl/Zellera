package zel.wired.box

sealed interface BoxKind {
    val saveMessageId: Int

    data object Trigger : BoxKind {
        override val saveMessageId = 3050
    }

    data object Effect : BoxKind {
        override val saveMessageId = 3051
    }

    data object Condition : BoxKind {
        override val saveMessageId = 3052
    }

    data object Addon : BoxKind {
        override val saveMessageId = 3055
    }

    data object Selector : BoxKind {
        override val saveMessageId = 3058
    }

    data object Variable : BoxKind {
        override val saveMessageId = 3059
    }
}
