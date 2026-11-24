package zel.room

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.protocol.typed.PacketPackage
import zel.room.port.RoomPort

class RoomEngine(
    private val registry: RoomRegistry,
    val packages: List<PacketPackage>,
    val rooms: RoomPort,
    private val furniSaved: () -> Long = { 0 },
    private val flushHeld: () -> Unit = {},
) {
    fun shutdown() {
        val before = furniSaved()
        val rooms = registry.loadedRoomIds().size
        registry.shutdown()
        flushHeld()
        val furni = furniSaved() - before
        log.info("shutdown: {} loaded rooms saved, {} furni rows handed to the database writer", rooms, furni)
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(RoomEngine::class.java)
    }
}
