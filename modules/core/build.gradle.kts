plugins {
    id("servified.module")
    application
}

dependencies {
    implementation(project(":utils"))
    implementation(project(":protocol"))
    implementation("io.github.dervarex.minified:minified-utils:4.1.0")
}

application {
    mainClass = "com.dervarex.servified.Main"
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.file("run").apply { mkdirs() }
    standardInput = System.`in`
}
