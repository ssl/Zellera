package zel.room.furni.behaviour.state

import zel.game.effects.EffectRoles
import zel.protocol.generated.ObjectUpdateOutCodec
import zel.protocol.generated.StuffData
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Ticking
import zel.room.furni.Usable
import zel.room.furni.behaviour.special.SpecialStuff
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Placeable
import zel.room.furni.place.StuffOnWire
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.util.MILLIS_PER_SECOND
import zel.util.SECONDS_PER_HOUR
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class PlantFurni(
    private val part: StatePart,
    private val store: PlantStore,
    private val persist: (Runnable) -> Unit,
    private val kind: (RoomFurni) -> PlantStore.Kind,
    private val cooldownSeconds: () -> Int = { DEFAULT_COOLDOWN_SECONDS },
    private val deathSeconds: () -> Int = { DEFAULT_DEATH_HOURS * SECONDS_PER_HOUR },
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / MILLIS_PER_SECOND },
) : Usable,
    Ticking,
    Placeable,
    StuffOnWire {
    private val growths = ConcurrentHashMap<Int, PlantStore.Growth>()

    override fun stuff(furni: RoomFurni): StuffData {
        val growth = growth(furni)
        val now = nowSeconds()
        val untilWater = if (growth.dead || growth.wateredAt <= 0) 0 else cooldownSeconds() - (now - growth.wateredAt)
        val anchor = if (growth.wateredAt > 0) growth.wateredAt else now
        val untilDeath = if (growth.dead) DEAD else deathSeconds() - (now - anchor)
        return SpecialStuff.of(
            furni,
            SpecialStuff.CRACKABLE,
            StuffData.Value.Crackable(
                frame(furni, growth).toString(),
                untilWater.coerceAtLeast(0).toInt(),
                if (growth.dead) DEAD.toInt() else untilDeath.coerceAtLeast(0).toInt(),
            ),
        )
    }

    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        val unit = scope.room.units.ofPlayer(playerId) ?: return
        val anchor = furni.anchor ?: return
        refresh(scope, furni, playerId)
        if (part.hooks.effect(unit) != EffectRoles.of(EffectRoles.WATER)) return
        if (abs(anchor.x - unit.tile.x) > 1 || abs(anchor.y - unit.tile.y) > 1) return
        val growth = growth(furni)
        val now = nowSeconds()
        if (growth.dead || (growth.wateredAt > 0 && now - growth.wateredAt < cooldownSeconds())) return
        growth.frame = minOf(growth.frame + 1, kind(furni).growFrames)
        growth.wateredAt = now
        changed(scope, furni, growth)
    }

    override fun onTick(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        // only a plant already read (it was shown or used): the tick holds the room's lock and never reads the store
        val growth = growths[furni.rowId] ?: return
        if (growth.dead) return
        val now = nowSeconds()
        val anchor = if (growth.wateredAt > 0) growth.wateredAt else return
        if (now - anchor < deathSeconds()) return
        growth.dead = true
        changed(scope, furni, growth)
    }

    override fun onPlaced(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        val growth = PlantStore.Growth(0, nowSeconds(), false)
        growths[furni.rowId] = growth
        changed(scope, furni, growth)
    }

    override fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        growths.remove(furni.rowId)
        val rowId = furni.rowId
        persist(Runnable { store.delete(rowId) })
    }

    private fun growth(furni: RoomFurni): PlantStore.Growth =
        growths.getOrPut(furni.rowId) { store.load(furni.rowId) ?: PlantStore.Growth(0, nowSeconds(), false) }

    private fun frame(
        furni: RoomFurni,
        growth: PlantStore.Growth,
    ): Int = if (growth.dead) kind(furni).deadFrame else growth.frame

    private fun changed(
        scope: RoomScope,
        furni: RoomFurni,
        growth: PlantStore.Growth,
    ) {
        val saved = growth.copy()
        val rowId = furni.rowId
        persist(Runnable { store.save(rowId, saved) })
        StateShow.whole(scope, furni, frame(furni, growth).toString())
    }

    private fun refresh(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
    ) {
        val update = FurniWire.objectUpdate(furni)
        scope.out.send(Lane.Now, Audience.Player(playerId), ObjectUpdateOutCodec, update)
    }

    companion object {
        private const val DEAD = -1L
        private const val DEFAULT_COOLDOWN_SECONDS = 1800
        private const val DEFAULT_DEATH_HOURS = 24
    }
}
