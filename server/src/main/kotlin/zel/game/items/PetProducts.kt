package zel.game.items

import java.util.concurrent.atomic.AtomicReference

/** The product names the content sells pets under, lowercase. */
object PetProducts {
    private val names = AtomicReference<Set<String>>(emptySet())

    fun install(all: Collection<String>) {
        names.set(all.filter { it.isNotBlank() }.mapTo(HashSet()) { it.lowercase() })
    }

    fun has(name: String): Boolean = name.lowercase() in names.get()
}
