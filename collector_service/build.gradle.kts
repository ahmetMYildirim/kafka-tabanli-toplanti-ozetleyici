import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    java
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("jacoco")
}

group = "org.example"
version = "0.0.1-SNAPSHOT"
description = "collector_service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.apache.kafka:kafka-streams")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("jakarta.validation:jakarta.validation-api:4.0.0-M1")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-junit-jupiter:5.21.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("net.dv8tion:JDA:5.2.1")
    implementation("club.minnced:opus-java:1.1.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("io.jsonwebtoken:jjwt:0.13.0")
    testImplementation("com.h2database:h2")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("com.google.cloud:google-cloud-vertexai:1.1.0")
    implementation("org.bytedeco:ffmpeg:7.1.1-1.5.12")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn("test")

    reports {
        xml.required.set(true)
        csv.required.set(true)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco"))
    }

    afterEvaluate {
        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/config/**",
                        "**/entity/**",
                        "**/dto/**",
                        "**/*Application.class"
                    )
                }
            })
        )
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("test")

    afterEvaluate {
        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/config/**",
                        "**/entity/**",
                        "**/dto/**",
                        "**/*Application.class"
                    )
                }
            })
        )
    }

    violationRules {
        rule {
            limit {
                minimum = "0.8".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn("jacocoTestCoverageVerification")
}

tasks.register("testReport") {
    group = "verification"
    description = "Generates comprehensive test and coverage reports"

    dependsOn("test", "jacocoTestReport")

    doLast {
        println("Test Raporları Oluşturuldu:")
        println("--> JUnit HTML: build/reports/tests/test/index.html")
        println("--> JaCoCo HTML: build/reports/jacoco/index.html")
        println("--> JaCoCo XML: build/reports/jacoco/jacocoTestReport.xml")
        println("--> JaCoCo CSV: build/reports/jacoco/jacocoTestReport.csv")
    }
}

tasks.bootBuildImage {
    runImage = "paketobuildpacks/ubuntu-noble-run-base:latest"
}
