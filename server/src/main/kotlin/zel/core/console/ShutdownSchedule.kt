package zel.core.console

class ShutdownSchedule {
    /** Epoch seconds, 0 while no stop is planned. */
    @Volatile
    var closesAt: Int = 0
}
