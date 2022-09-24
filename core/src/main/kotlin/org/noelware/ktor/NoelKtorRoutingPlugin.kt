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
import io.ktor.server.websocket.*
import org.noelware.ktor.annotations.ExperimentalApi
import org.noelware.ktor.loader.ListBasedLoader

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
            if (route.isWebSocketRoute) {
                log.debug("Found WebSocket route ${route.path} to register!")
                val keyPair = Pair(route.path, HttpMethod.parse("Connect"))
                if (alreadyRegistered.contains(keyPair)) {
                    log.warn("WebSocket route ${route.path} was already registered! Skipping...")
                    continue
                }

                alreadyRegistered.add(keyPair)
                routing.webSocket("${if (pluginConfig.basePrefix == "/") "" else pluginConfig.basePrefix.removeSuffix("/")}/${route.path.removePrefix("/")}".removeSuffix("/"), null) {
                    route.websocketCall(this)
                }
            } else {
                log.debug("Found route ${route.path} with methods [${route.method.joinToString(", ") { it.value }}] to register!")
                for (method in route.method) {
                    val keyPair = Pair(route.path, method)
                    if (alreadyRegistered.contains(keyPair)) {
                        log.warn("Route ${method.value} ${route.path} was already registered! Skipping...")
                        continue
                    }

                    alreadyRegistered.add(keyPair)
                    routing.route("${if (pluginConfig.basePrefix == "/") "" else pluginConfig.basePrefix.removeSuffix("/")}/${route.path.removePrefix("/")}".removeSuffix("/"), method) {
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
