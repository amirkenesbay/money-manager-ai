import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
}

group = "ai.moneymanager"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

sourceSets {
    main {
        java {
            srcDirs("src/main/kotlin")
        }
        kotlin {
            srcDirs("src/main/kotlin")
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

val chatMachinistVersion: String by project
val chatMachinistMongoVersion: String by project

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.chatmachinist:chat-machinist:$chatMachinistVersion")
    implementation("com.chatmachinist:chat-machinist-mongo-persistence-starter:$chatMachinistMongoVersion")

    // Google Gemini AI SDK
    implementation("com.google.genai:google-genai:1.34.0")

    // Jackson Kotlin module for data class deserialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        javaParameters.set(true) // Required for Gemini function calling
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}