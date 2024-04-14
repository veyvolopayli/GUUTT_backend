import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "org.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

val sshAntTask = configurations.create("sshAntTask")

val exposedVersion: String by project
val http4kVersion: String by project

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    runtimeOnly("org.http4k:http4k-bom:$http4kVersion")
    implementation("org.http4k:http4k-core:$http4kVersion")
    implementation("org.http4k:http4k-server-undertow:$http4kVersion")
    implementation("org.http4k:http4k-client-apache:$http4kVersion")
    implementation("org.http4k:http4k-server-jetty:$http4kVersion")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    sshAntTask("org.apache.ant:ant-jsch:1.10.12")
    implementation("net.sourceforge.tess4j:tess4j:5.11.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("ch.qos.logback:logback-classic:1.5.5")
}

tasks.withType<ShadowJar> {
    manifest {
        attributes(
            "Main-Class" to "org.example.MainKt"
        )
    }
}

ant.withGroovyBuilder {
    "taskdef"(
        "name" to "scp",
        "classname" to "org.apache.tools.ant.taskdefs.optional.ssh.Scp",
        "classpath" to configurations.get("sshAntTask").asPath
    )
    "taskdef"(
        "name" to "ssh",
        "classname" to "org.apache.tools.ant.taskdefs.optional.ssh.SSHExec",
        "classpath" to configurations.get("sshAntTask").asPath
    )
}

task("deploy") {
    dependsOn("clean", "shadowJar")
    ant.withGroovyBuilder {
        doLast {
            val knownHosts = File.createTempFile("knownhosts", "txt")
            val user = "root"
            val host = "5.181.255.253"
            val key = file("keys/GUUTT-backend-key")
            val jarFileName = "GUUTT_backend-1.0.0-all.jar"
            try {
                "scp"(
                    "file" to file("src/main/kotlin/tesseract/tessdata/eng.traineddata"),
                    "todir" to "$user@$host:/root/guutt/tessdata",
                    "keyfile" to key,
                    "trust" to true,
                    "knownhosts" to knownHosts
                )
                "scp"(
                    "file" to file("build/libs/$jarFileName"),
                    "todir" to "$user@$host:/root/guutt",
                    "keyfile" to key,
                    "trust" to true,
                    "knownhosts" to knownHosts
                )
                "ssh"(
                    "host" to host,
                    "username" to user,
                    "keyfile" to key,
                    "trust" to true,
                    "knownhosts" to knownHosts,
                    "command" to "mv /root/guutt/$jarFileName /root/guutt/guutt.jar"
                )
                "ssh"(
                    "host" to host,
                    "username" to user,
                    "keyfile" to key,
                    "trust" to true,
                    "knownhosts" to knownHosts,
                    "command" to "systemctl stop guutt"
                )
                "ssh"(
                    "host" to host,
                    "username" to user,
                    "keyfile" to key,
                    "trust" to true,
                    "knownhosts" to knownHosts,
                    "command" to "systemctl start guutt"
                )
            } finally {
                knownHosts.delete()
            }
        }
    }
}
