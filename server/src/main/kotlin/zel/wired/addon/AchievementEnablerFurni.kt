package zel.wired.addon

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.furni.place.Placeable
import zel.wired.protocol.BoxUse

class AchievementEnablerFurni(
    private val use: BoxUse,
    private val environment: AchievementEnvironment,
) : Usable by use,
    Placeable {
    override fun onPlaced(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        use.onPlaced(scope, furni)
        environment.resend(scope)
    }

    override fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) = environment.resend(scope)
}
