import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
}

group = "ru.mipt.npm"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/space/maven")
}

val ktorVersion = "1.6.4"

dependencies {
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")
    implementation("org.jetbrains:space-sdk-jvm:98244-beta")
    implementation("ch.qos.logback:logback-classic:1.2.10")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("ru.mipt.npm.space.documentextractor.MainKt")
}