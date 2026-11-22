plugins {
    id("java")
}

group = "com.dervarex.servified"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("io.github.dervarex.minified:minified-utils:4.1.0")
}

tasks.test {
    useJUnitPlatform()
}