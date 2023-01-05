/*
 * 📭 ktor-routing: Extensions to Ktor’s routing system to add object-oriented routing and much more.
 * Copyright (c) 2022-2023 Noelware <team@noelware.org>
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

package org.noelware.ktor.plugin

import dev.floofy.utils.slf4j.logging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import org.noelware.ktor.endpoints.AbstractEndpoint
import org.noelware.ktor.loader.IEndpointLoader
import org.noelware.ktor.loader.ListBasedLoader

/**
 * Main entrypoint to Noelware's Ktor routing plugin. You can register the plugin as:
 * ```kotlin
 * install(NoelKtorPlugin) {
 *    /* insert options here */
 * }
 * ```
 */
@KtorDsl
public class NoelKtorRouting private constructor(private val config: Configuration) {
    private val log by logging<NoelKtorRouting>()

    @KtorDsl
    public class Configuration {
        /**
         * When using [basePrefix], this option will allow to include a trailing slash
         * at the end of the route. It will register as both `/api` and `/api/` if this
         * option is true.
         */
        public var includeTrailingSlash: Boolean = false

        /**
         * Represents the endpoint loader used to load additional endpoints
         * from a different location source.
         */
        public var endpointLoader: IEndpointLoader? = null

        /**
         * Represents all the available endpoints that can be used to inject
         * endpoints when configuring the [NoelKtorRouting] plugin.
         */
        public val endpoints: MutableList<AbstractEndpoint> = mutableListOf()

        /**
         * Represents the relative prefix used to be
         */
        public var basePrefix: String = "/"

        /**
         * Registers multiple endpoints at once
         * @param endpoints All the endpoints
         */
        public fun endpoints(vararg endpoints: AbstractEndpoint) {
            this.endpoints.addAll(endpoints)
        }

        /**
         * Registers a new [IEndpointLoader] to use to load all the routes.
         * @param loader The loader to use
         * @return this [configuration object][Configuration] to chain methods.
         */
        public fun endpointLoader(loader: IEndpointLoader) {
            this.endpointLoader = loader
        }
    }

    private fun toUrl(path: String): String = if (config.basePrefix == "/") {
        path
    } else {
        "${config.basePrefix.removeSuffix("/")}/${path.removeSuffix("/")}"
    }

    private fun install(pipeline: Application) {
        log.info("Installing NoelKtorRouting into the base application!")

        val routing = pipeline.pluginOrNull(Routing) ?: run {
            log.warn("Routing plugin was not registered, so I registered it for you!")
            pipeline.routing {}
        }

        val loader = config.endpointLoader ?: ListBasedLoader(config.endpoints)
        log.debug("Using endpoint loader => ${loader::class.simpleName}")

        val endpointsToRegister = loader.load().toMutableList()
        if (config.endpoints.isNotEmpty()) {
            endpointsToRegister += config.endpoints
        }

        // Keep a cache of what was already registered since it will attempt
        // to register duplicate entries.
        val alreadyRegistered: MutableList<Pair<String, HttpMethod>> = mutableListOf()

        log.info("Now registering ${endpointsToRegister.size} endpoints to Ktor...")
        for (endpoint in endpointsToRegister) {
            log.debug("Found endpoint class '${endpoint::class.simpleName}' to register with ${endpoint.routes.size} routes to register!")
            for (route in endpoint.routes) {
                log.debug("Loading route '${route.path}' with method [${route.methods.joinToString(", ") { it.value }}] (is websocket route: ${route.isWebSocketRoute})")
                if (route.isWebSocketRoute) {
                    if (!pipeline.pluginRegistry.contains(WebSockets.key)) {
                        throw IllegalStateException("You will need to enable the WebSockets plugin to use this feature")
                    }

                    val keyPair = route.path to HttpMethod("Connect")
                    if (alreadyRegistered.contains(keyPair)) {
                        log.warn("WebSocket route '${route.path}' was already registered! Skipping...")
                        continue
                    }

                    alreadyRegistered.add(keyPair)
                    routing.webSocket(toUrl(route.path)) {
                        log.debug("Shooting and persisting WebSocket call [${route.path}]")
                        route.websocketCall(this)
                    }

                    continue
                }

                for (method in route.methods) {
                    val keyPair = route.path to method
                    if (alreadyRegistered.contains(keyPair)) {
                        log.warn("Route '${method.value} ${route.path}' was already registered!")
                        continue
                    }

                    alreadyRegistered.add(keyPair)

                    val self = this
                    if (config.includeTrailingSlash) {
                        routing.route(toUrl(route.path), method) {
                            // Install all the global plugins
                            for ((plugin, configure) in endpoint.plugins) {
                                self.log.debug("Installing global-scoped plugin ${plugin.key.name} on route ${method.value} ${route.path}!")
                                if (pluginRegistry.contains(plugin.key)) {
                                    self.log.warn("Plugin ${plugin.key.name} was already registered on route ${method.value} ${route.path}! Skipping.")
                                    continue
                                }

                                install(plugin, configure)
                            }

                            // Install all the local route-scoped plugins
                            for ((plugin, configure) in route.plugins) {
                                self.log.debug("Installing local-scoped plugin ${plugin.key.name} on route ${method.value} ${route.path}!")
                                if (pluginRegistry.contains(plugin.key)) {
                                    self.log.warn("Plugin ${plugin.key.name} was already registered on route ${method.value} ${route.path}! Skipping.")
                                    continue
                                }

                                install(plugin, configure)
                            }

                            handle { route.run(call) }
                        }
                    }

                    routing.route(toUrl(route.path).removeSuffix("/"), method) {
                        // Install all the global plugins
                        for ((plugin, configure) in endpoint.plugins) {
                            self.log.debug("Installing global-scoped plugin ${plugin.key.name} on route ${method.value} ${route.path}!")
                            if (pluginRegistry.contains(plugin.key)) {
                                self.log.warn("Plugin ${plugin.key.name} was already registered on route ${method.value} ${route.path}! Skipping.")
                                continue
                            }

                            install(plugin, configure)
                        }

                        // Install all the local route-scoped plugins
                        for ((plugin, configure) in route.plugins) {
                            self.log.debug("Installing local-scoped plugin ${plugin.key.name} on route ${method.value} ${route.path}!")
                            if (pluginRegistry.contains(plugin.key)) {
                                self.log.warn("Plugin ${plugin.key.name} was already registered on route ${method.value} ${route.path}! Skipping.")
                                continue
                            }

                            install(plugin, configure)
                        }

                        handle { route.run(call) }
                    }

                    log.debug("Registered endpoint '${method.value} ${route.path}' successfully!")
                }
            }
        }
    }

    public companion object: BaseApplicationPlugin<Application, Configuration, NoelKtorRouting> {
        override val key: AttributeKey<NoelKtorRouting> = AttributeKey("NoelKtorPlugin")
        override fun install(pipeline: Application, configure: Configuration.() -> Unit): NoelKtorRouting = NoelKtorRouting(Configuration().apply(configure)).apply {
            install(pipeline)
        }
    }
}
