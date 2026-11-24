package zel.wired.variable

import zel.wired.firing.HeldValue

class StoredValues(
    val users: Map<Int, Map<Int, HeldValue>>,
    val furni: Map<Int, Map<Int, HeldValue>>,
    val rooms: Map<Int, HeldValue>,
)
