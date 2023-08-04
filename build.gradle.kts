import groovy.lang.GroovyObject
import org.jetbrains.exposed.gradle.isReleaseBuild
import org.jetbrains.exposed.gradle.setPomMetadata
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm") apply true
    id("io.gitlab.arturbosch.detekt") version "1.23.0"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.13.2"
    id("org.jetbrains.dokka") version "1.8.20"
    id("maven-publish")
    id ("java")
    idea
}

apiValidation {
    ignoredProjects.addAll(listOf("exposed-tests", "exposed-bom"))
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

allprojects {
    if (this.name != "exposed-tests" && this.name != "exposed-bom" && this != rootProject) {
        apply(plugin = "maven-publish")
        apply(plugin = "java")

        val projekt = this

        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/tpasipanodya/exposed")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR")
                        password = System.getenv("GITHUB_TOKEN")
                    }
                }
            }
            publications {
                create<MavenPublication>("mavenJava") {
                    artifactId = projekt.name
                    from(projekt.components["java"])
                    version = if (isReleaseBuild()) "${projekt.version}" else "${projekt.version}-SNAPSHOT-"
                    versionMapping {
                        usage("java-api") {
                            fromResolutionOf("runtimeClasspath")
                        }
                    }
                    pom { setPomMetadata(projekt) }
                }
            }
        }
    }
}

subprojects {
    tasks.withType<KotlinJvmCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "19"
            apiVersion = "1.7"
            languageVersion = "1.7"
        }
    }
}
