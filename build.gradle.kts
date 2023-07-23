import groovy.lang.GroovyObject
import org.jetbrains.exposed.gradle.isReleaseBuild
import org.jetbrains.exposed.gradle.setPomMetadata
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") apply true
    id("org.jetbrains.dokka") version "1.8.20"
    id("maven-publish")
    idea
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
                    version = if (isReleaseBuild()) "${projekt.version}" else "${projekt.version}-SNAPSHOT"
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
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "20"
        }
    }
}
