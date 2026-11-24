package zel.game.players.inventory

import zel.game.players.Player
import zel.protocol.outgoing.inventory.PlayerEffectActivated
import zel.protocol.outgoing.inventory.PlayerEffectAdded
import zel.protocol.outgoing.inventory.PlayerEffectExpired
import zel.server.GameServices
import java.sql.Connection
import java.sql.SQLException

class AvatarEffects(
    private val services: GameServices,
    private val player: Player,
) {
    private val store = AvatarEffectStore(services.database.dataSource)
    private val playerId: Int get() = player.profile.id
    private val byId = HashMap<Int, AvatarEffect>()

    /** Effects put on since they were last stored; written when the inventory is let go. Guarded by [byId]. */
    private val unsaved = HashSet<Int>()

    @Volatile
    var worn = 0
        private set

    init {
        store.allOf(playerId).forEach { byId[it.id] = it }
    }

    val owned: List<AvatarEffect>
        get() =
            synchronized(byId) {
                settle(services.unixTimestamp())
                byId.values.toList()
            }

    fun owns(effectId: Int): Boolean = synchronized(byId) { effectId in byId }

    fun ownsPermanent(effectId: Int): Boolean = synchronized(byId) { byId[effectId]?.isPermanent == true }

    fun grant(
        effectId: Int,
        duration: Int = AvatarEffect.TEMPORARY_SECONDS,
    ): AvatarEffect =
        synchronized(byId) {
            settle(services.unixTimestamp())
            val owned = byId[effectId]
            val effect =
                if (owned == null) {
                    AvatarEffect(effectId, playerId, duration).also(store::add)
                } else {
                    owned.addCopy(duration)?.also {
                        store.write(it)
                        unsaved.remove(effectId)
                    } ?: owned
                }
            show(effect)
            effect
        }

    fun accept(effect: AvatarEffect) =
        synchronized(byId) {
            unsaved.remove(effect.id)
            show(effect)
        }

    fun wear(effectId: Int) {
        val now = services.unixTimestamp()
        val effect =
            synchronized(byId) {
                settle(now)
                val owned = byId[effectId] ?: return
                val usable = owned.isPermanent || owned.copies > 0
                if (!owned.isActivated && !usable) player.client?.send(PlayerEffectExpired(owned))
                (if (usable) owned.wornFrom(now) else owned).also {
                    byId[effectId] = it
                    if (it !== owned) unsaved.add(effectId)
                }
            }
        worn = effectId
        // Activated effects grey out dancing until they expire, so a permanent one is put on without an activation.
        if (!effect.isPermanent) player.client?.send(PlayerEffectActivated(effect))
        player.rooms.giveEffect(playerId, effectId, effect.secondsLeft(now))
    }

    fun takeOff() {
        worn = 0
        player.rooms.giveEffect(playerId, 0)
    }

    fun remove(effectId: Int) {
        val effect =
            synchronized(byId) {
                unsaved.remove(effectId)
                byId.remove(effectId)
            } ?: return
        store.remove(effect)
        if (worn == effectId) takeOff()
        player.client?.send(PlayerEffectExpired(effect))
    }

    fun dispose() {
        synchronized(byId) {
            settle(services.unixTimestamp())
            store.writeAll(playerId, unsaved.mapNotNull(byId::get))
            unsaved.clear()
            byId.clear()
        }
    }

    /**
     * Spends every running copy whose time is up and stores it at once, so a later write never brings the spent copy
     * back; an effect without copies left goes. Called under [byId].
     */
    private fun settle(now: Int) {
        val spent = byId.values.mapNotNull { effect -> effect.spentBy(now).takeIf { it != effect } }
        spent.forEach { effect ->
            unsaved.remove(effect.id)
            if (effect.copies > 0) {
                byId[effect.id] = effect
                store.write(effect)
            } else {
                byId.remove(effect.id)
                store.remove(effect)
            }
            if (worn == effect.id) worn = 0
            player.client?.send(PlayerEffectExpired(effect))
        }
    }

    private fun show(effect: AvatarEffect) {
        byId[effect.id] = effect
        player.client?.send(PlayerEffectAdded(effect))
    }

    companion object {
        @Throws(SQLException::class)
        fun storePurchase(
            connection: Connection,
            playerId: Int,
            effectId: Int,
            duration: Int,
        ): AvatarEffect = AvatarEffectStore.addBoughtCopy(connection, playerId, effectId, duration)
    }
}
