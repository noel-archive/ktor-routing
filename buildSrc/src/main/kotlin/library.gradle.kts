/*
 * 📭 ktor-routing: Extensions to Ktor’s routing system to add object-oriented routing and much more.
 * Copyright (c) 2022-2023 Noelware, LLC. <team@noelware.org>
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

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.noelware.ktor.gradle.*
import dev.floofy.utils.gradle.*
import java.io.StringReader
import java.util.Properties
import java.net.URL

plugins {
    id("com.diffplug.spotless")
    id("org.jetbrains.dokka")
    kotlin("jvm")

    `maven-publish`
    `java-library`
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
    api("org.slf4j:slf4j-api:2.0.6")

    // Noel Utils
    implementation("dev.floofy.commons:slf4j:2.5.0")

    // Testing Utilities
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("org.slf4j:slf4j-simple:2.0.6")
    testImplementation(kotlin("test"))

    if (project.name.contains("loader-")) {
        api(project(":core"))
    }
}

kotlin {
    explicitApi()
}

spotless {
    kotlin {
        licenseHeaderFile("${rootProject.projectDir}/assets/HEADING")
        trimTrailingWhitespace()
        endWithNewline()

        ktlint().apply {
            setUseExperimental(true)
            setEditorConfigPath(file("${rootProject.projectDir}/.editorconfig"))
        }
    }
}

java {
    sourceCompatibility = JAVA_VERSION
    targetCompatibility = JAVA_VERSION
}

val fullPath = path.substring(1).replace(':', '-')
// transforms :core -> core

tasks {
    withType<Jar> {
        manifest {
            attributes(
                "Implementation-Version" to "$VERSION",
                "Implementation-Vendor" to "Noelware, LLC. [team@noelware.org]",
                "Implementation-Title" to "ktor-routing"
            )
        }
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JAVA_VERSION.toString()
        kotlinOptions.javaParameters = true
        kotlinOptions.freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        outputs.upToDateWhen { false }
        maxParallelForks = Runtime.getRuntime().availableProcessors()
        failFast = true // kill gradle if a test fails

        testLogging {
            events.addAll(listOf(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED))
            showStandardStreams = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    dokkaHtml {
        dokkaSourceSets {
            configureEach {
                platform.set(org.jetbrains.dokka.Platform.jvm)
                jdkVersion.set(JAVA_VERSION.majorVersion.toInt())
                //includes.from("DokkaDescription.md")

                sourceLink {
                    remoteLineSuffix by "#L"
                    localDirectory by file("src/main/kotlin")
                    remoteUrl by uri("https://github.com/Noelware/ktor-routing/tree/master/${project.name}/src/main/kotlin").toURL()
                }

                // Link to Ktor
                externalDocumentationLink {
                    packageListUrl by uri("https://api.ktor.io/package-list").toURL()
                    url by uri("https://api.ktor.io").toURL()
                }
            }
        }
    }
}

// Get the `publishing.properties` file from the `gradle/` directory
// in the root project.
val publishingPropsFile = file("${rootProject.projectDir}/gradle/publishing.properties")
val publishingProps = Properties()

// If the file exists, let's get the input stream
// and load it.
if (publishingPropsFile.exists()) {
    publishingProps.load(publishingPropsFile.inputStream())
} else {
    // Check if we do in environment variables
    val accessKey = System.getenv("NOELWARE_PUBLISHING_ACCESS_KEY") ?: ""
    val secretKey = System.getenv("NOELWARE_PUBLISHING_SECRET_KEY") ?: ""

    if (accessKey.isNotEmpty() && secretKey.isNotEmpty()) {
        val data = """
        |s3.accessKey=$accessKey
        |s3.secretKey=$secretKey
        """.trimMargin()

        publishingProps.load(StringReader(data))
    }
}

// Check if we have the `NOELWARE_PUBLISHING_ACCESS_KEY` and `NOELWARE_PUBLISHING_SECRET_KEY` environment
// variables, and if we do, set it in the publishing.properties loader.
val snapshotRelease: Boolean = run {
    val env = System.getenv("NOELWARE_PUBLISHING_IS_SNAPSHOT") ?: "false"
    env == "true"
}

val fullName = path.substring(1).replace(':', '-')
val sourcesJar by tasks.registering(Jar::class) {
    archiveBaseName by "ktor-routing"
    archiveClassifier by "sources"

    from(sourceSets.main.get().allSource)
}

val dokkaJar by tasks.registering(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assemble Kotlin documentation with Dokka"

    archiveBaseName by "ktor-routing"
    archiveClassifier by "javadoc"

    from(tasks.dokkaHtml)
    dependsOn(tasks.dokkaHtml)
}

publishing {
    publications {
        create<MavenPublication>("ktor") {
            from(components["kotlin"])

            artifactId = "ktor-routing-$fullName"
            groupId = "org.noelware.ktor"
            version = "$VERSION"

            artifact(sourcesJar.get())
            artifact(dokkaJar.get())

            pom {
                description by "Extension to Ktor’s routing system to add object-oriented routing and much more."
                name by project.name
                url by "https://ktor.noelware.org"

                organization {
                    name by "Noelware"
                    url by "https://noelware.org"
                }

                developers {
                    developer {
                        name by "Noel"
                        email by "cutie@floofy.dev"
                        url by "https://floofy.dev"
                    }

                    developer {
                        name by "Noelware Team"
                        email by "team@noelware.org"
                        url by "https://noelware.org"
                    }
                }

                issueManagement {
                    system by "GitHub"
                    url by "https://github.com/Noelware/ktor-routing/issues"
                }

                licenses {
                    license {
                        name by "Apache-2.0"
                        url by "http://www.apache.org/licenses/LICENSE-2.0"
                    }
                }

                scm {
                    connection by "scm:git:ssh://github.com/Noelware/ktor-routing.git"
                    developerConnection by "scm:git:ssh://git@github.com:Noelware/ktor-routing.git"
                    url by "https://github.com/Noelware/ktor-routing"
                }
            }
        }
    }

    repositories {
        val url = if (snapshotRelease) "s3://august/noelware/maven/snapshots" else "s3://august/noelware/maven"
        maven(url) {
            credentials(AwsCredentials::class.java) {
                accessKey = publishingProps.getProperty("s3.accessKey") ?: System.getenv("NOELWARE_PUBLISHING_ACCESS_KEY") ?: ""
                secretKey = publishingProps.getProperty("s3.secretKey") ?: System.getenv("NOELWARE_PUBLISHING_SECRET_KEY") ?: ""
            }
        }
    }
}
