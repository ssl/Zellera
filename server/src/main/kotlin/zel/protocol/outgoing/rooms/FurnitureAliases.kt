package zel.protocol.outgoing.rooms

import zel.content.ClientStrings
import zel.protocol.generated.FurnitureAliasesOut
import zel.protocol.generated.FurnitureAliasesOutCodec
import zel.protocol.outgoing.OutgoingMessage

class FurnitureAliases : OutgoingMessage<FurnitureAliasesOut>(FurnitureAliasesOutCodec) {
    override fun message() = FurnitureAliasesOut(aliases(ClientStrings.snapshot()))

    internal companion object {
        fun aliases(profile: Map<String, String>): List<FurnitureAliasesOut.List1Item> =
            profile
                .filterKeys { it.startsWith(ClientStrings.FURNI_ALIASES_PREFIX) }
                .entries
                .sortedBy { it.key.removePrefix(ClientStrings.FURNI_ALIASES_PREFIX).toIntOrNull() ?: Int.MAX_VALUE }
                .flatMap { it.value.split(',') }
                .mapNotNull { pair ->
                    val (name, alias) = pair.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
                    FurnitureAliasesOut.List1Item(name.trim(), alias.trim())
                }
    }
}
