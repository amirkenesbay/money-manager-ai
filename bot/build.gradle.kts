plugins {
    kotlin("kapt")
}

val chatMachinistVersion: String by project
val chatMachinistMongoVersion: String by project

dependencies {
    implementation(project(":core"))
    implementation("com.chatmachinist:chat-machinist:$chatMachinistVersion")
    implementation("com.chatmachinist:chat-machinist-mongo-persistence-starter:$chatMachinistMongoVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.github.sps.junidecode:junidecode:0.3")
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(testFixtures(project(":core")))
    testImplementation(kotlin("test"))
}
