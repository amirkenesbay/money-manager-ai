plugins {
    `java-library`
    `java-test-fixtures`
    kotlin("kapt")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-data-mongodb")
    api("io.micrometer:micrometer-core")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.google.genai:google-genai:1.34.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.github.sps.junidecode:junidecode:0.3")
    implementation("org.dhatim:fastexcel:0.18.4")
    implementation("org.dhatim:fastexcel-reader:0.18.4")
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(kotlin("test"))
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
}
