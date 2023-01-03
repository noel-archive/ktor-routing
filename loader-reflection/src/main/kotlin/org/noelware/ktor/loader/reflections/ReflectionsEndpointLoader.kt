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

package org.noelware.ktor.loader.reflections

import org.noelware.ktor.endpoints.AbstractEndpoint
import org.noelware.ktor.loader.IEndpointLoader
import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder

/**
 * [Endpoint loader][IEndpointLoader] based off the Reflections library to load without
 * any dependency injection.
 */
public class ReflectionsEndpointLoader(builder: ConfigurationBuilder.() -> Unit = {}): IEndpointLoader {
    private val reflections = Reflections(ConfigurationBuilder().apply(builder))

    public constructor(packageName: String): this({
        forPackage(packageName)
    })

    override fun load(): List<AbstractEndpoint> {
        val cls = reflections.getSubTypesOf(AbstractEndpoint::class.java)
        val instances = mutableListOf<AbstractEndpoint>()

        for (c in cls) {
            // based off assumption, we might as well guess that
            // this is barely used without any constructor arguments
            //
            // if you're going to use constructor arguments, i recommend like...
            // using a DI engine (or submit a PR why and how we can design it)
            val ctor = c.getConstructor()
            instances.add(ctor.newInstance())
        }

        return instances
    }
}
