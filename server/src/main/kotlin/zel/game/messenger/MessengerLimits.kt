package zel.game.messenger

import zel.core.rules.rules

data class MessengerLimits(
    val offlineDeliveryMax: Int = rules.offlineMessagesMax,
)
