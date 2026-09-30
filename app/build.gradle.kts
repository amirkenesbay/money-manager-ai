plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":bot"))
    implementation(project(":web"))
}

tasks.named<Jar>("jar") {
    enabled = false
}
