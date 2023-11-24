plugins {
    kotlin("jvm") apply true
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(20)
}

dependencies {
    api(project(":exposed-core"))
}
