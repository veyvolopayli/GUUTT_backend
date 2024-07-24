plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.9.22"
    id("io.ktor.plugin") version "2.3.12"
}

group = "org.guutt"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(18)
}

val exposedVersion: String by project
val http4kVersion: String by project

application {
    mainClass.set("org.guutt.MainKt")
}

ktor {
    fatJar {
        archiveFileName.set("guutt.jar")
    }
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
//    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    runtimeOnly("org.http4k:http4k-bom:$http4kVersion")
    implementation("org.http4k:http4k-core:$http4kVersion")
    implementation("org.http4k:http4k-server-undertow:$http4kVersion")
    implementation("org.http4k:http4k-client-apache:$http4kVersion")
    implementation("org.http4k:http4k-server-jetty:$http4kVersion")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("net.sourceforge.tess4j:tess4j:5.11.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("ch.qos.logback:logback-classic:1.5.5")
}

