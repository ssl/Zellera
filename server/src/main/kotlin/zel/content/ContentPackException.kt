package zel.content

class ContentPackException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
