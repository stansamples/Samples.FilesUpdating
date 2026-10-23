repositories.mavenCentral()

plugins {
    kotlin("jvm")
}

tasks.register<JavaExec>("run") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "org.stansamples.filesupdating.AppKt"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0")
}
