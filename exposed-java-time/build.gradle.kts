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
    jvmToolchain(20)
}

dependencies {
    api(project(":exposed-core"))
    testImplementation(project(":exposed-dao"))
    testImplementation(project(":exposed-tests"))
    testImplementation(project(":exposed-json"))
    testImplementation("junit", "junit", "4.12")
    testImplementation(kotlin("test-junit"))
}

tasks.withType<Test>().configureEach {
    testLogging {
        events.addAll(listOf(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED))
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}
