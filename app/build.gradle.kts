repositories.mavenCentral()

plugins {
    kotlin("jvm")
}

val specifics by properties

sourceSets {
    getByName("main") {
        setOf(specifics).forEach { name ->
            kotlin.srcDirs("src/$name/kotlin")
        }
    }
}

tasks.register<JavaExec>("run") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "org.stansamples.filesupdating.AppKt"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0")
}
