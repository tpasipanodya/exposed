# exposed-bom
Bill of Materials for all Exposed modules

# Maven
```xml
<repositories>
    <repository>
        <id>mavenCentral</id>
        <name>mavenCentral</name>
        <url>https://maven.pkg.github.com/tpasipanodya/exposed</url>
    </repository>
</repositories>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.taff.exposed</groupId>
            <artifactId>exposed-bom</artifactId>
            <version>0.10.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.taff.exposed</groupId>
        <artifactId>exposed-core</artifactId>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>io.taff.exposed</groupId>
        <artifactId>exposed-dao</artifactId>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>io.taff.exposed</groupId>
        <artifactId>exposed-jdbc</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

# Gradle
```kotlin
repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/tpasipanodya/exposed")
        credentials {
            username = System.getenv("GITHUB_USERNAME")
            password = System.getenv("GITHUB_ACCESS_TOKEN")
        }
    }
}

dependencies {
    implementation(platform("io.taff.exposed:exposed-bom:0.10.0"))
    implementation("io.taff.exposed", "exposed-core")
    implementation("io.taff.exposed", "exposed-dao")
    implementation("io.taff.exposed", "exposed-jdbc")
}
```
