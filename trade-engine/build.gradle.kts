import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "com.marketintelligence.tradeengine"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.marketintelligence.tradeengine.MainKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = "com.marketintelligence.tradeengine.MainKt"
    }
}

dependencies {
    implementation(libs.kiteconnect)
    implementation(libs.gson)
    implementation(libs.caffeine)

    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.server.netty.jvm)
    implementation(libs.ktor.server.content.negotiation.jvm)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Logging for Ktor/Netty
    implementation(libs.slf4j.simple)

    // Testing
    testImplementation("junit:junit:4.13.2")
}

tasks.register<Sync>("syncToAppLibs") {
    // This task now explicitly depends on the shadowJar task and takes its output as the source
    dependsOn(tasks.named("shadowJar"))
    from(tasks.named("shadowJar").get().outputs.files)
    into(rootProject.project(":app").file("libs"))
}
