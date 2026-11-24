package zel.wired.addon

import zel.wired.box.BoxSettings
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class TextOutputUsername(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val names: UnitNames,
) : PlaceholderBox {
    private val text = PlaceholderText(settings)

    override val name: String get() = text.name

    override fun text(firing: Firing): String? =
        text.join(Targets.units(firing, this).asSequence().mapNotNull { names.nameOf(firing.scope, it) })
}
