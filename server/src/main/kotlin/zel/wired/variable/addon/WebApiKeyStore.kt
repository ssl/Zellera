package zel.wired.variable.addon

interface WebApiKeyStore {
    fun keys(boxId: Int): Map<String, StoredApiKey>

    fun save(
        boxId: Int,
        kind: String,
        roomId: Int,
        userId: Int,
        key: StoredApiKey,
        createdAt: Long,
    )

    fun delete(
        boxId: Int,
        kind: String,
    )

    fun deleteAll(boxId: Int)
}
