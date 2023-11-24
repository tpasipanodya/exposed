import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    gradleApi()
    implementation("org.jetbrains.kotlin.jvm", "org.jetbrains.kotlin.jvm.gradle.plugin", "1.9.20")
    implementation("com.avast.gradle", "gradle-docker-compose-plugin", "0.17.4")
}

plugins {
    `kotlin-dsl` apply true
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "20"
        apiVersion = "1.9"
        languageVersion = "1.9"
    }
}

gradlePlugin {
    plugins {
        create("testWithDBs") {
            id = "testWithDBs"
            implementationClass = "org.jetbrains.exposed.gradle.DBTestingPlugin"
        }
    }
}
