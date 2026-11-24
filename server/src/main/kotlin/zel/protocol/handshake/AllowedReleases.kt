package zel.protocol.handshake

import zel.protocol.typed.ClientVersions

class AllowedReleases(
    setting: String?,
) {
    private val names: List<String> =
        setting
            .orEmpty()
            .trim()
            .ifEmpty { ANY }
            .split(';', ',')
            .map { it.trim() }

    /** A hello that names no version never may; the setting lists version numbers. */
    fun admits(release: String): Boolean {
        val number = ClientVersions.number(release.trim()) ?: return false
        return names.any {
            it == number.toString() || (it.equals(ANY, ignoreCase = true) && number in ClientVersions.all)
        }
    }

    companion object {
        const val ANY = "any"
    }
}
