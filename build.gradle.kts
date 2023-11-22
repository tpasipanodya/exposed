import org.jetbrains.exposed.gradle.isReleaseBuild
import org.jetbrains.exposed.gradle.setPomMetadata
import org.jetbrains.exposed.gradle.testDb
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.exposed.gradle.Versions

plugins {
    kotlin("jvm") apply true
    id("org.jetbrains.dokka") version "1.9.10"
    id("com.avast.gradle.docker-compose")
    id("maven-publish")
    id ("java")
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
    tasks.withType<KotlinJvmCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "19"
            apiVersion = "1.9"
            languageVersion = "1.9"
        }
    }
}

subprojects {
    if (name == "exposed-bom") return@subprojects

    apply(plugin = "org.jetbrains.kotlin.jvm")

    testDb("h2") {
        withContainer = false
        dialects("H2", "H2_MYSQL", "H2_PSQL", "H2_MARIADB", "H2_ORACLE", "H2_SQLSERVER")

        dependencies {
            dependency("com.h2database:h2:${Versions.h2_v2}")
        }
    }

    testDb("h2_v1") {
        withContainer = false
        dialects("H2", "H2_MYSQL")

        dependencies {
            dependency("com.h2database:h2:${Versions.h2}")
        }
    }

    testDb("sqlite") {
        withContainer = false
        dialects("sqlite")

        dependencies {
            dependency("org.xerial:sqlite-jdbc:${Versions.sqlLite3}")
        }
    }

    testDb("mysql") {
        port = 3001
        dialects("mysql")
        dependencies {
            dependency("mysql:mysql-connector-java:${Versions.mysql51}")
        }
    }

    testDb("mysql8") {
        port = 3002
        dialects("mysql")
        dependencies {
            dependency("mysql:mysql-connector-java:${Versions.mysql80}")
        }
    }

    testDb("mariadb_v2") {
        dialects("mariadb")
        container = "mariadb"
        port = 3000
        dependencies {
            dependency("org.mariadb.jdbc:mariadb-java-client:${Versions.mariaDB_v2}")
        }
    }

    testDb("mariadb_v3") {
        dialects("mariadb")
        container = "mariadb"
        port = 3000
        dependencies {
            dependency("org.mariadb.jdbc:mariadb-java-client:${Versions.mariaDB_v3}")
        }
    }

    testDb("oracle") {
        port = 3003
        colima = true
        dialects("oracle")
        dependencies {
            dependency("com.oracle.database.jdbc:ojdbc8:${Versions.oracle12}")
        }
    }

    testDb("postgres") {
        port = 3004
        dialects("postgresql")
        dependencies {
            dependency("org.postgresql:postgresql:${Versions.postgre}")
        }
    }

    testDb("postgresNG") {
        port = 3004
        dialects("postgresqlng")
        container = "postgres"
        dependencies {
            dependency("org.postgresql:postgresql:${Versions.postgre}")
            dependency("com.impossibl.pgjdbc-ng:pgjdbc-ng:${Versions.postgreNG}")
        }
    }

    testDb("sqlserver") {
        port = 3005
        dialects("sqlserver")
        dependencies {
            dependency("com.microsoft.sqlserver:mssql-jdbc:${Versions.sqlserver}")
        }
    }
}
