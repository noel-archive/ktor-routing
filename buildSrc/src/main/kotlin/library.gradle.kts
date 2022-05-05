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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.noelware.ktor.gradle.*
import dev.floofy.utils.gradle.*

plugins {
    id("com.diffplug.spotless")
    id("org.jetbrains.dokka")
    id("io.kotest")
    kotlin("jvm")
}

group = "org.noelware.ktor"
version = "$VERSION"

repositories {
    mavenCentral()
    mavenLocal()
    noel()
}

dependencies {
    // SLF4J for logging
    api("org.slf4j:slf4j-api:1.7.36")

    // Noel Utils
    implementation("dev.floofy.commons:slf4j:2.1.0")

    // Testing utilities
    testImplementation("io.kotest:kotest-runner-junit5:5.3.0")
    testImplementation("io.kotest:kotest-assertions-core:5.3.0")
    testImplementation("io.kotest:kotest-property:5.3.0")

    if (project.name.contains("loader-")) {
        api(project(":core"))
    }
}

spotless {
    kotlin {
        trimTrailingWhitespace()
        licenseHeaderFile("${rootProject.projectDir}/assets/HEADING")
        endWithNewline()

        // We can't use the .editorconfig file, so we'll have to specify it here
        // issue: https://github.com/diffplug/spotless/issues/142
        // ktlint 0.35.0 (default for Spotless) doesn't support trailing commas
        ktlint("0.43.0")
            .userData(
                mapOf(
                    "no-consecutive-blank-lines" to "true",
                    "no-unit-return" to "true",
                    "disabled_rules" to "no-wildcard-imports,colon-spacing",
                    "indent_size" to "4"
                )
            )
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JAVA_VERSION.toString()
        kotlinOptions.javaParameters = true
        kotlinOptions.freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }

    dokkaHtml {
        dokkaSourceSets {
            configureEach {
                platform.set(org.jetbrains.dokka.Platform.jvm)
                jdkVersion.set(17)
                //includes.from("DokkaDescription.md")

                sourceLink {
                    localDirectory.set(file("src/main/kotlin"))
                    remoteUrl.set(uri("https://github.com/Noelware/ktor-routing/tree/master/${project.name}/src/main/kotlin").toURL())
                    remoteLineSuffix.set("#L")
                }
            }
        }
    }
}

java {
    sourceCompatibility = JAVA_VERSION
    targetCompatibility = JAVA_VERSION
}