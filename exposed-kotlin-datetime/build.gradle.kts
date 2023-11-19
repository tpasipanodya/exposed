import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm") apply true
    kotlin("plugin.serialization") apply true
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(19)
}

dependencies {
    api(project(":exposed-core"))
    api("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    testImplementation(project(":exposed-dao"))
    testImplementation(project(":exposed-tests"))
    testImplementation(project(":exposed-json"))
    testImplementation("junit", "junit", "4.12")
    testImplementation(kotlin("test-junit"))
}

//tasks.withType<KotlinJvmCompile>().configureEach {
//    kotlinOptions {
//        jvmTarget = "16"
//        apiVersion = "1.5"
//        languageVersion = "1.5"
//    }
//}

tasks.withType<Test>().configureEach {
    testLogging {
        events.addAll(listOf(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED))
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}
