/*
 * 📭 ktor-routing: Extensions to Ktor’s routing system to add object-oriented routing and much more.
 * Copyright (c) 2022 Noelware <team@noelware.org>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.noelware.ktor

import io.ktor.server.application.*
import io.ktor.util.*
import org.noelware.ktor.annotations.ExperimentalApi
import org.noelware.ktor.endpoints.AbstractEndpoint
import org.noelware.ktor.loader.IEndpointLoader

/**
 * Represents a caller function.
 * @param T The result type
 * @param E The exception type
 */
typealias DataResponseCallee<T, E> = suspend (ApplicationCall, T, E?) -> Unit

@KtorDsl
class NoelKtorRoutingConfiguration {
    internal var endpoints = mutableListOf<AbstractEndpoint>()
    internal var endpointLoader: IEndpointLoader? = null
    internal var dataResponseCallee: DataResponseCallee<Result<*>, Throwable>? = null

    /**
     * If the library should support handling [kotlin.Result][kotlin.Result] objects if returned
     * from any endpoint method.
     */
    @ExperimentalApi
    var supportKotlinResult: Boolean = false

    /**
     * Returns the base prefix to use when appending routes from an [AbstractEndpoint] class.
     */
    var basePrefix: String = "/"

    /**
     * Registers a new [IEndpointLoader] to use to load all the routes.
     * @param loader The loader to use
     * @return this [configuration object][NoelKtorRoutingConfiguration] to chain methods.
     */
    fun endpointLoader(loader: IEndpointLoader): NoelKtorRoutingConfiguration {
        if (endpointLoader != null) {
            throw IllegalStateException("An endpoint loader is already injected!")
        }

        endpointLoader = loader
        return this
    }

    /**
     * Registers a list of [endpoints][AbstractEndpoint] to this configuration object.
     * @param _endpoints The endpoints to register at runtime
     * @return this [configuration object][NoelKtorRoutingConfiguration] to chain methods.
     */
    fun endpoints(vararg _endpoints: AbstractEndpoint): NoelKtorRoutingConfiguration {
        endpoints += _endpoints
        return this
    }

    /**
     * Sets the [DataResponseCallee] to when [kotlin.Result][kotlin.Result] is supported when
     * returned by any endpoint method. This can be only added if [supportKotlinResult] is
     * `true`, otherwise you're fine by not using this. It is also recommended to use this
     * method so the plugin knows how to handle the result, otherwise it'll throw away
     * the result.
     *
     * @param callee The caller function to use
     * @return this [configuration][NoelKtorRoutingConfiguration] to chain methods
     * @since 0.3-beta (30.06.22)
     */
    @ExperimentalApi
    @Suppress("UNCHECKED_CAST")
    fun dataResponse(callee: DataResponseCallee<Result<Any>, Exception?>): NoelKtorRoutingConfiguration {
        check(supportKotlinResult) { "You must enable the experimental `supportKotlinResult` property before using this method." }

        if (dataResponseCallee != null) {
            throw IllegalStateException("`dataResponseCallee` was already added.")
        }

        dataResponseCallee = callee as DataResponseCallee<Result<*>, Throwable>
        return this
    }
}
