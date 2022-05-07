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
import kotlin.reflect.full.callSuspend

@KtorDsl
class NoelKtorRoutingConfiguration {
    var endpoints = mutableListOf<AbstractEndpoint>()
    var endpointLoader: IEndpointLoader? = null

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

val NoelKtorRoutingPlugin: ApplicationPlugin<NoelKtorRoutingConfiguration> = createApplicationPlugin(
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
        for ((path, method, callable) in endpoint.routes) {
            routing.route(path, method) {
                log.debug("Registering route ${method.value} $path!")

                val keyPair = Pair(path, method)
                if (alreadyRegistered.contains(keyPair)) {
                    log.debug("Route ${method.value} $path is already registered, skipping.")
                    return@route
                }

                alreadyRegistered.add(keyPair)

                // Install all the global scoped plugins for this route.
                for ((plugin, configure) in endpoint.plugins) {
                    log.debug("Installed global-scoped plugin ${plugin.key.name} onto route $path")

                    if (pluginRegistry.contains(plugin.key)) {
                        log.warn("Plugin ${plugin.key.name} is already registered on route, skipping!")
                        continue
                    }

                    install(plugin, configure)
                }

                // Get all the plugins for this specific route
                val plugins = endpoint.specificPlugins.filter { it.first == path }
                for ((_, plugin, configure) in plugins) {
                    log.debug("Installed local-scoped plugin $plugin onto route $path")
                    if (pluginRegistry.contains(plugin.key)) {
                        log.warn("Plugin ${plugin.key.name} is already registered on route, skipping!")
                        continue
                    }

                    install(plugin, configure)
                }

                handle {
                    if (callable.isSuspend) {
                        callable.callSuspend(endpoint, call)
                    } else {
                        callable.call(endpoint, call)
                    }
                }
            }
        }
    }
}
