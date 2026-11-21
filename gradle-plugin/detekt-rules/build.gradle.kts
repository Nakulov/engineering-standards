plugins {
    kotlin("jvm")
}

val detektVersion = "2.0.0-alpha.6"

dependencies {
    compileOnly("dev.detekt:detekt-api:$detektVersion")

    testImplementation("dev.detekt:detekt-api:$detektVersion")
    testImplementation("dev.detekt:detekt-test:$detektVersion")
    testImplementation("io.github.nakulov:kotestkit:2.2.0")
}

tasks.test {
    useJUnitPlatform()
}
