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

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.util.*
import org.noelware.ktor.endpoints.AbstractEndpoint
import org.noelware.ktor.loader.IEndpointLoader
import org.noelware.ktor.loader.ListBasedLoader
import org.slf4j.LoggerFactory

@KtorDsl
class NoelKtorRoutingConfiguration {
    internal var endpoints = mutableListOf<AbstractEndpoint>()
    internal var endpointLoader: IEndpointLoader? = null

    /**
     * Registers a new [IEndpointLoader] to use to load all the routes.
     * @param loader The loader to use
     * @return this [configuration object][NoelKtorRoutingConfiguration] to chain methods.
     */
    fun endpointLoader(loader: IEndpointLoader): NoelKtorRoutingConfiguration {
        if (endpointLoader != null)
            throw IllegalStateException("An endpoint loader is already injected!")

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
    val log = LoggerFactory.getLogger("org.noelware.ktor.NoelKtorRoutingPluginKt")
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
        endpoint.init()

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
                        route.run(call)
                    }
                }
            }
        }
    }
}

@Deprecated("Using 'NoelKtorRoutingPlugin' is deprecated.",
    replaceWith = ReplaceWith("NoelKtorRouting"))
val NoelKtorRoutingPlugin = NoelKtorRouting
