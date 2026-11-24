package zel.game.items

import zel.core.Configuration
import zel.core.threading.GameThreads
import zel.game.catalog.Catalog
import zel.game.pets.PetsModule
import zel.protocol.generated.StuffData
import java.util.Random
import java.util.concurrent.ThreadLocalRandom
import java.util.function.Supplier
import javax.sql.DataSource

class ItemServices(
    val threading: GameThreads,
    val config: Configuration,
    val dataSource: DataSource,
    private val items: Supplier<Items?>,
    private val catalog: Supplier<Catalog?>,
    private val pets: Supplier<PetsModule?>,
) {
    /** The room engine installs the one that knows wired boxes. */
    @Volatile
    var inventoryStuff: InventoryStuff = InventoryStuff { StuffData(0, StuffData.Value.Legacy(it.state), null) }

    fun random(): Random = ThreadLocalRandom.current()

    fun items(): Items = requireNotNull(items.get()) { "the items are not built yet" }

    fun catalog(): Catalog = requireNotNull(catalog.get()) { "the catalog is not built yet" }

    fun pets(): PetsModule = requireNotNull(pets.get()) { "the pets are not built yet" }
}
