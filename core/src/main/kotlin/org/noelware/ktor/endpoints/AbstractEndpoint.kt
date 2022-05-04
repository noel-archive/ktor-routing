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
import kotlin.reflect.KCallable
import kotlin.reflect.full.findAnnotation

/**
 * Represents an endpoint to use to implement routing in an object-orientated way.
 * @param path The path that will be subsequently merged through the HTTP pipeline.
 */
open class AbstractEndpoint(val path: String = "/") {
    private val log = LoggerFactory.getLogger(this::class.java)
    val routes: MutableList<Triple<String, HttpMethod, KCallable<*>>> = mutableListOf()
    val plugins = mutableListOf<Pair<Plugin<Route, *, *>, Any.() -> Unit>>()

    // this looks like actual hot garbage but it works, i think.
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
                val p = mergePaths(path, meta.path)
                log.debug("Registered route ${m.value} $p")

                routes.add(Triple(p, m, method))
            }
        }

        for (method in getMethods) {
            val meta = method.findAnnotation<Get>() ?: continue
            val p = mergePaths(path, meta.path)
            log.debug("Registered route GET $p")

            routes.add(Triple(p, HttpMethod.Get, method))
        }

        for (method in putMethods) {
            val meta = method.findAnnotation<Put>() ?: continue
            val p = mergePaths(path, meta.path)
            log.debug("Registered route PUT $p")

            routes.add(Triple(p, HttpMethod.Get, method))
        }

        for (method in postMethods) {
            val meta = method.findAnnotation<Post>() ?: continue
            val p = mergePaths(path, meta.path)
            log.debug("Registered route POST $p")

            routes.add(Triple(p, HttpMethod.Get, method))
        }

        for (method in deleteMethods) {
            val meta = method.findAnnotation<Delete>() ?: continue
            val p = mergePaths(path, meta.path)
            log.debug("Registered route DELETE $p")

            routes.add(Triple(p, HttpMethod.Get, method))
        }

        for (method in patchMethods) {
            val meta = method.findAnnotation<Patch>() ?: continue
            val p = mergePaths(path, meta.path)
            log.debug("Registered route PATCH $p")

            routes.add(Triple(p, HttpMethod.Get, method))
        }
    }

    private fun mergePaths(current: String, other: String): String =
        if (other == "/") current else "${if (current == "/") "" else current}$other"

    /**
     * Installs a route plugin onto this [AbstractEndpoint] that will be used.
     * @param plugin The plugin to install
     * @param configure The configuration block to configure this [plugin].
     * @return this [AbstractEndpoint] to chain methods.
     */
    fun <C: Any, B: Any> install(plugin: Plugin<Route, C, B>, configure: C.() -> Unit): AbstractEndpoint {
        @Suppress("UNCHECKED_CAST")
        plugins.add(Pair(plugin, configure as Any.() -> Unit))
        return this
    }
}
