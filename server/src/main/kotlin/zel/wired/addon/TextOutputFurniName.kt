package zel.wired.addon

import zel.wired.box.BoxSettings
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class TextOutputFurniName(
    override val furniId: Int,
    override val settings: BoxSettings,
) : PlaceholderBox {
    private val text = PlaceholderText(settings)

    override val name: String get() = text.name

    override fun text(firing: Firing): String? =
        text.join(
            Targets.furni(firing, this).asSequence().map { it.definition.displayName.ifEmpty { it.definition.name } },
        )
}
