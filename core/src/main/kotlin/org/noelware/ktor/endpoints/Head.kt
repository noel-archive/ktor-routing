package org.noelware.ktor.endpoints

/**
 * Represents a HEAD request that can be accessed.
 */
annotation class Head(
    /**
     * The path that is used to identify this endpoint.
     */
    val path: String = "/"
)
