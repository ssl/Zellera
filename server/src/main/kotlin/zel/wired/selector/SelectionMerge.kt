package zel.wired.selector

import zel.wired.box.BoxSettings

object SelectionMerge {
    fun <T> apply(
        selected: MutableSet<T>,
        settings: BoxSettings,
        matched: Set<T>,
        candidates: () -> Collection<T>,
        unconfigured: Boolean = false,
    ) {
        when {
            settings.filter && settings.invert -> {
                selected.removeAll(matched)
            }

            settings.filter -> {
                selected.retainAll(matched)
            }

            settings.invert -> {
                if (!unconfigured) selected.addAll(candidates().filterNot { it in matched })
            }

            else -> {
                selected.addAll(matched)
            }
        }
    }
}
