apply(plugin = "java-library")
apply(plugin = "maven-publish")
apply(plugin = "signing")

_java {
    withJavadocJar()
    withSourcesJar()
}

val version: String by rootProject
