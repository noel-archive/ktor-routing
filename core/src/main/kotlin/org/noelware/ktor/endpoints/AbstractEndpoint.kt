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

package org.noelware.ktor.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import kotlin.reflect.full.findAnnotation
import org.noelware.ktor.internal.Route as NoelRoute

/**
 * Represents an endpoint to use to implement routing in an object-orientated way.
 * @param paths A list of HTTP resources that will be subsequently merged within the routing plugin.
 */
open class AbstractEndpoint(val paths: List<String> = listOf("/")) {
    private val log = LoggerFactory.getLogger(this::class.java)
    internal val routes: MutableList<NoelRoute> = mutableListOf()
    internal val plugins = mutableListOf<Pair<Plugin<Route, *, *>, Any.() -> Unit>>()

    /**
     * Represents an endpoint to use to implement routing in an object-orientated way.
     * @param path A single resource prefix that will correspond to this [AbstractEndpoint].
     */
    constructor(path: String): this(listOf(path))

    init {
        log.debug("Finding all routes based off annotations...")

        val httpMethods = this::class.members.filter { it.findAnnotation<Http>() != null }
        val getMethods = this::class.members.filter { it.findAnnotation<Get>() != null }
        val putMethods = this::class.members.filter { it.findAnnotation<Put>() != null }
        val patchMethods = this::class.members.filter { it.findAnnotation<Patch>() != null }
        val postMethods = this::class.members.filter { it.findAnnotation<Post>() != null }
        val deleteMethods = this::class.members.filter { it.findAnnotation<Delete>() != null }

        log.debug("Found ${httpMethods.size} multiplexed routes, ${getMethods.size} GET routes, ${putMethods.size} PUT routes, ${patchMethods.size} PATCH routes, ${postMethods.size} POST routes, and ${deleteMethods.size} DELETE routes!")

        for (method in httpMethods) {
            val meta = method.findAnnotation<Http>() ?: continue
            val methods = meta.methods.map { HttpMethod.parse(it) }

            for (m in methods) {
                for (path in paths) {
                    val p = mergePaths(path, meta.path)
                    log.debug("Registered route ${m.value} $p")
                    routes.add(
                        NoelRoute(
                            p,
                            methods,
                            method,
                            this
                        )
                    )
                }
            }
        }

        for (method in getMethods) {
            for (path in paths) {
                val meta = method.findAnnotation<Get>() ?: continue
                val p = mergePaths(path, meta.path)
                log.debug("Registered route => GET $p")

                routes.add(
                    NoelRoute(
                        p,
                        HttpMethod.Get,
                        method,
                        this
                    )
                )
            }
        }

        for (method in putMethods) {
            for (path in paths) {
                val meta = method.findAnnotation<Put>() ?: continue
                val p = mergePaths(path, meta.path)
                log.debug("Registered route => PUT $p")

                routes.add(
                    NoelRoute(
                        p,
                        HttpMethod.Put,
                        method,
                        this
                    )
                )
            }
        }

        for (method in postMethods) {
            for (path in paths) {
                val meta = method.findAnnotation<Post>() ?: continue
                val p = mergePaths(path, meta.path)
                log.debug("Registered route => POST $p")

                routes.add(
                    NoelRoute(
                        p,
                        HttpMethod.Post,
                        method,
                        this
                    )
                )
            }
        }

        for (method in deleteMethods) {
            for (path in paths) {
                val meta = method.findAnnotation<Delete>() ?: continue
                val p = mergePaths(path, meta.path)
                log.debug("Registered route => DELETE $p")

                routes.add(
                    NoelRoute(
                        p,
                        HttpMethod.Delete,
                        method,
                        this
                    )
                )
            }
        }

        for (method in patchMethods) {
            for (path in paths) {
                val meta = method.findAnnotation<Patch>() ?: continue
                val p = mergePaths(path, meta.path)
                log.debug("Registered route => PATCH $p")

                routes.add(
                    NoelRoute(
                        p,
                        HttpMethod.Patch,
                        method,
                        this
                    )
                )
            }
        }
    }

    private fun mergePaths(current: String, other: String): String =
        if (other == "/") current else "${if (current == "/") "" else current}$other"

    /**
     * Installs a route plugin into every route listed in the [endpoints] map
     * with a mapping of `method -> route`.
     *
     * @param endpoints The endpoints mapped from `method -> route` to register it from.
     * @param plugin The plugin to install
     * @param configure The configuration block to configure this [plugin].
     * @return this [AbstractEndpoint] to chain methods.
     */
    fun <C: Any, B: Any> install(
        endpoints: Map<HttpMethod, String>,
        plugin: Plugin<Route, C, B>,
        configure: C.() -> Unit = {}
    ): AbstractEndpoint {
        for ((method, route) in endpoints) {
            install(method, route, plugin, configure)
        }

        return this
    }

    /**
     * Installs a route plugin into every route listed in the [routes] list
     * with the methods that are registered with it.
     *
     * @param routes A list of routes to register it on, regardless of the method.
     * @param plugin The plugin to install
     * @param configure The configuration block to configure this [plugin].
     * @return this [AbstractEndpoint] to chain methods.
     */
    fun <C: Any, B: Any> install(
        routes: List<String>,
        plugin: Plugin<Route, C, B>,
        configure: C.() -> Unit = {}
    ): AbstractEndpoint {
        for (route in routes) {
            install(route, plugin, configure)
        }

        return this
    }

    /**
     * Installs a route plugin into every route that is installed on this [AbstractEndpoint].
     *
     * @param plugin The plugin to install
     * @param configure The configuration block to configure this [plugin].
     * @return this [AbstractEndpoint] to chain methods.
     */
    fun <C: Any, B: Any> install(plugin: Plugin<Route, C, B>, configure: C.() -> Unit = {}): AbstractEndpoint {
        log.debug("Initialized plugin ${plugin.key.name} to every route.")

        @Suppress("UNCHECKED_CAST")
        plugins.add(Pair(plugin, configure as Any.() -> Unit))
        return this
    }

    /**
     * Installs a route plugin on a specific route that is installed on this [AbstractEndpoint].
     * @param route The route to install it on
     * @param plugin The plugin to install
     * @param configure The configuration block to configure this [plugin].
     * @return this [AbstractEndpoint] to chain methods.
     */
    fun <C: Any, B: Any> install(route: String, plugin: Plugin<Route, C, B>, configure: C.() -> Unit = {}): AbstractEndpoint {
        log.debug("Initialized plugin ${plugin.key.name} on route $route")

        val r = routes.filter { it.path == route }
        for (rou in r) {
            log.debug("Installing plugin ${plugin.key.name} on routes ${rou.method.joinToString(", ") { it.value }} ${rou.path}!")
            rou.install(plugin, configure)
        }

        return this
    }

    /**
     * Installs a route plugin on a specific route AND [method][HttpMethod] that is installed on this [AbstractEndpoint].
     * @param method The HTTP method to use to find the route to install the plugin in.
     * @param route The route to install it on
     * @param plugin The plugin to install
     * @param configure The configuration block to configure this [plugin].
     * @return this [AbstractEndpoint] to chain methods.
     */
    fun <C: Any, B: Any> install(
        method: HttpMethod,
        route: String,
        plugin: Plugin<Route, C, B>,
        configure: C.() -> Unit = {}
    ): AbstractEndpoint {
        log.debug("Adding plugin ${plugin.key.name} to route ${method.value} $route")

        val rou = routes.firstOrNull { it.path == route && it.method.contains(method) }
            ?: throw IllegalStateException("Route ${method.value} $route doesn't exist.")

        rou.install(plugin, configure)
        return this
    }
}
