package zel.protocol.incoming.guilds

import zel.protocol.typed.PacketIds

internal object GuildInput {
    const val MAX_NAME_LENGTH = 29
    const val MAX_DESCRIPTION_LENGTH = 254

    fun areIds(vararg ids: Int): Boolean = ids.all(PacketIds::isId)

    fun isValidName(name: String?): Boolean = name != null && name.isNotBlank() && name.length <= MAX_NAME_LENGTH

    fun isValidDescription(description: String?): Boolean =
        description != null && description.length <= MAX_DESCRIPTION_LENGTH
}
