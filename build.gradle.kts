import java.nio.file.Files
import java.nio.file.Paths

repositories {
    mavenCentral()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.noarg") version "2.1.10"
    kotlin("plugin.allopen") version "2.1.10"
    application

    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
}

noArg {
    annotation("jakarta.persistence.Entity")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("org.springframework.context.annotation.Configuration")
    annotation("org.springframework.stereotype.Controller")
    annotation("org.springframework.stereotype.Service")
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("org.springframework.boot:spring-boot-starter") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.10")
    implementation("org.liquibase:liquibase-core:4.31.1")

    runtimeOnly("com.h2database:h2:2.3.232")

    testImplementation(kotlin("test"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.0")
    testImplementation("org.assertj:assertj-core:3.27.3")
}

configurations {
    all {
        exclude(module = "logback-classic")
        exclude(module = "spring-boot-starter-logging")
    }
}

tasks.wrapper {
    gradleVersion = "8.13"
    distributionType = Wrapper.DistributionType.BIN
}

tasks.bootJar {
    archiveBaseName.set("atm-simulator")
    archiveVersion.set(Files.readString(Paths.get("version.txt")).trim())
    archiveClassifier.set("")
}

tasks.bootRun {
    standardInput = System.`in` // Allow interactive input
}

tasks.test {
    useJUnitPlatform()
}