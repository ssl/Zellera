package zel.room.tick

import jdk.jfr.Category
import jdk.jfr.Description
import jdk.jfr.Event
import jdk.jfr.Label
import jdk.jfr.Name
import jdk.jfr.StackTrace

@Name("zel.RoomTick")
@Label("Room tick")
@Category("Zellera")
@Description("One 500 ms tick of a loaded room, under the room lock")
@StackTrace(false)
internal class RoomTickEvent : Event() {
    @Label("Room")
    var roomId: Int = 0

    @Label("Units")
    var units: Int = 0
}
