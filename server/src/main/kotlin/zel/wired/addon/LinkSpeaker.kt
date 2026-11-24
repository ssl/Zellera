package zel.wired.addon

import zel.wired.box.AddonBox
import zel.wired.box.BoxSettings

class LinkSpeaker(
    override val furniId: Int,
    override val settings: BoxSettings,
) : AddonBox {
    val saysMessages: Boolean get() = settings.intAt(0, 1) != 0
    val saysFrames: Boolean get() = settings.intAt(1, 1) != 0

    fun listensTo(linkId: Int): Boolean = linkId in settings.picks

    companion object {
        const val CODE = "wf_xtra_link_speaker"
    }
}
