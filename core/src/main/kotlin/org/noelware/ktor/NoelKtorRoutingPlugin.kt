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

import dev.floofy.utils.slf4j.logging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.util.*
import org.noelware.ktor.annotations.ExperimentalApi
import org.noelware.ktor.endpoints.AbstractEndpoint
import org.noelware.ktor.loader.IEndpointLoader
import org.noelware.ktor.loader.ListBasedLoader

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

/**
 * The plugin to register into your Ktor application.
 *
 * ```kotlin
 * fun Application.module() {
 *    install(NoelKtorRouting)
 * }
 * ```
 */
val NoelKtorRouting: ApplicationPlugin<NoelKtorRoutingConfiguration> = createApplicationPlugin(
    "NoelKtorRouting",
    ::NoelKtorRoutingConfiguration
) {
    val routing = application.pluginOrNull(Routing) ?: error("Missing `routing {}` plugin in application module")
    val log by logging("org.noelware.ktor.routing.NoelKtorRoutingPlugin")
    val endpointsMap = pluginConfig.endpoints
    val loader = if (pluginConfig.endpointLoader == null) {
        ListBasedLoader(endpointsMap)
    } else {
        pluginConfig.endpointLoader!!
    }

    val newEndpointMap = loader.load()
    val alreadyRegistered = mutableListOf<Pair<String, HttpMethod>>()
    log.debug("Registering ${newEndpointMap.size} endpoints!")

    for (endpoint in newEndpointMap) {
        log.debug("Found ${endpoint::class} to register!")
        for (route in endpoint.routes) {
            log.debug("Found route ${route.path} with methods [${route.method.joinToString(", ") { it.value }}] to register!")
            for (method in route.method) {
                routing.route(route.path, method) {
                    val keyPair = Pair(route.path, method)
                    if (alreadyRegistered.contains(keyPair)) {
                        log.warn("Route ${method.value} ${route.path} was already registered! Skipping...")
                        return@route
                    }

                    alreadyRegistered.add(keyPair)

                    // Install all the global plugins
                    for ((plugin, configure) in endpoint.plugins) {
                        log.debug("Installing global-scoped plugin ${plugin.key.name} on route ${method.value} ${route.path}!")
                        if (pluginRegistry.contains(plugin.key)) {
                            log.warn("Plugin ${plugin.key.name} was already registered on route ${method.value} ${route.path}! Skipping.")
                            continue
                        }

                        install(plugin, configure)
                    }

                    // Install all the local route-scoped plugins
                    for ((plugin, configure) in route.plugins) {
                        log.debug("Installing local-scoped plugin ${plugin.key.name} on route ${method.value} ${route.path}!")
                        if (pluginRegistry.contains(plugin.key)) {
                            log.warn("Plugin ${plugin.key.name} was already registered on route ${method.value} ${route.path}! Skipping.")
                            continue
                        }

                        install(plugin, configure)
                    }

                    handle {
                        handleRouteCall(this@createApplicationPlugin.pluginConfig, call, route)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalApi::class)
private suspend fun handleRouteCall(
    config: NoelKtorRoutingConfiguration,
    call: ApplicationCall,
    route: org.noelware.ktor.internal.Route
) {
    val log by logging("org.noelware.ktor.NoelKtorRoutingPluginKt\$handleRouteCall")
    val result = route.run(call)
    if (result != null && result is Unit) {
        return
    }

    // Support for Kotlin's Result (because why not?)
    if (config.supportKotlinResult && result is Result<*>) {
        if (config.dataResponseCallee == null) {
            log.warn("Throwing away result due to no data response caller function.")
            return
        }

        config.dataResponseCallee!!.invoke(call, result, result.exceptionOrNull())
    }
}
