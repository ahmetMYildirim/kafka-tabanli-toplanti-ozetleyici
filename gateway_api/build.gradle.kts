import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

plugins {
    java
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("jacoco")
    id("io.qameta.allure") version "2.11.2"
}

group = "org.example"
version = "1.0.0"
description = "gateway_api"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    implementation("com.bucket4j:bucket4j-core:8.7.0")

    // MySQL & JPA - Database access
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.mysql:mysql-connector-j:8.2.0")

    // OpenAPI (Swagger) - Auto-generates API documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // R2DBC SPI - Spring Boot autoconfigure classpath sorunu için
    implementation("io.r2dbc:r2dbc-spi:1.0.0.RELEASE")

    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito:mockito-junit-jupiter:5.21.0")
    testImplementation("io.qameta.allure:allure-junit5:2.24.0")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Test bulunamadığında hata vermeyi devre dışı bırak
    filter {
        isFailOnNoMatchingTests = false
    }
    // Gradle 9+ için test bulunamadığında hata vermeyi devre dışı bırak
    @Suppress("UnstableApiUsage")
    testLogging {
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn("test")

    reports {
        xml.required.set(true)
        csv.required.set(true)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco"))
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/config/**",
                    "**/model/**",
                    "**/dto/**",
                    "**/*Application.class"
                )
            }
        })
    )
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("test")

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/config/**",
                    "**/model/**",
                    "**/dto/**",
                    "**/*Application.class"
                )
            }
        })
    )

    violationRules {
        rule {
            limit {
                minimum = "0.0".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn("jacocoTestCoverageVerification")
}

allure {
    version.set("2.24.0")
    adapter {
        autoconfigure.set(true)
        aspectjWeaver.set(false)
    }
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

tasks.register("fullReport") {
    group = "verification"
    description = "Generates all reports including Allure"

    dependsOn("test", "jacocoTestReport", "allureReport")

    doLast {
        println("=".repeat(60))
        println("TÜM RAPORLAR OLUŞTURULDU")
        println("=".repeat(60))
        println("JUnit HTML    : build/reports/tests/test/index.html")
        println("JaCoCo HTML   : build/reports/jacoco/index.html")
        println("Allure HTML   : build/reports/allure-report/allureReport/index.html")
        println("=".repeat(60))
    }
}

tasks.register("generatePdfReport") {
    group = "verification"
    description = "Generates a professional PDF-ready HTML test report"

    dependsOn("test", "jacocoTestReport")

    doLast {
        val testResultsDir = file("build/test-results/test")
        val templateFile = file("src/test/resources/test-report-template.html")
        val outputFile = file("build/reports/test-report.html")
        val jacocoCsvFile = file("build/reports/jacoco/test/jacocoTestReport.csv")

        var totalTests = 0
        var passedTests: Int
        var failedTests = 0
        val testDetails = StringBuilder()
        val testSuites = mutableMapOf<String, Pair<Int, Boolean>>()

        testResultsDir.listFiles()?.filter { it.extension == "xml" }?.forEach { xmlFile ->
            val content = xmlFile.readText()

            val testsMatch = Regex("tests=\"(\\d+)\"").find(content)
            val failuresMatch = Regex("failures=\"(\\d+)\"").find(content)
            val errorsMatch = Regex("errors=\"(\\d+)\"").find(content)

            val tests = testsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val failures = failuresMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val errors = errorsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

            totalTests += tests
            failedTests += failures + errors

            val testCaseRegex = Regex("<testcase name=\"([^\"]+)\"[^>]*classname=\"([^\"]+)\"[^>]*time=\"([^\"]+)\"[^>]*(/?>)")
            testCaseRegex.findAll(content).forEach { match ->
                val testName = match.groupValues[1]
                val className = match.groupValues[2].substringAfterLast(".")
                val time = match.groupValues[3]
                val isFailed = content.contains("<failure") || content.contains("<error")

                val status = if (isFailed) "failed" else "passed"
                val statusText = if (isFailed) "Başarısız" else "Başarılı"

                testDetails.append("""
                    <tr>
                        <td>$testName</td>
                        <td><span class="duration">${time}s</span></td>
                        <td><span class="status-badge $status">$statusText</span></td>
                    </tr>
                """.trimIndent())

                val suiteKey = className
                val current = testSuites[suiteKey] ?: Pair(0, true)
                testSuites[suiteKey] = Pair(current.first + 1, current.second && !isFailed)
            }
        }

        passedTests = totalTests - failedTests
        val successRate = if (totalTests > 0) (passedTests * 100 / totalTests) else 0

        // JaCoCo coverage hesapla
        var coverage = 0
        if (jacocoCsvFile.exists()) {
            val lines = jacocoCsvFile.readLines()
            if (lines.size > 1) {
                var totalInstructions = 0L
                var coveredInstructions = 0L
                lines.drop(1).forEach { line ->
                    val cols = line.split(",")
                    if (cols.size >= 5) {
                        val missed = cols[3].toLongOrNull() ?: 0
                        val covered = cols[4].toLongOrNull() ?: 0
                        totalInstructions += missed + covered
                        coveredInstructions += covered
                    }
                }
                if (totalInstructions > 0) {
                    coverage = ((coveredInstructions * 100) / totalInstructions).toInt()
                }
            }
        }

        val suitesHtml = StringBuilder()
        testSuites.forEach { (suite, data) ->
            val status = if (data.second) "passed" else "failed"
            val statusText = if (data.second) "Başarılı" else "Başarısız"
            suitesHtml.append("""
                <tr>
                    <td>$suite</td>
                    <td>${data.first}</td>
                    <td><span class="status-badge $status">$statusText</span></td>
                </tr>
            """.trimIndent())
        }

        var html = templateFile.readText()
        val reportDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", Locale("tr")))
        html = html.replace("{{REPORT_DATE}}", reportDate)
        html = html.replace("{{TOTAL_TESTS}}", totalTests.toString())
        html = html.replace("{{PASSED_TESTS}}", passedTests.toString())
        html = html.replace("{{FAILED_TESTS}}", failedTests.toString())
        html = html.replace("{{SUCCESS_RATE}}", successRate.toString())
        html = html.replace("{{COVERAGE}}", coverage.toString())
        html = html.replace("{{TEST_SUITES}}", suitesHtml.toString())
        html = html.replace("{{TEST_DETAILS}}", testDetails.toString())

        outputFile.writeText(html)

        println("=".repeat(60))
        println("✅ PDF-READY TEST RAPORU OLUŞTURULDU!")
        println("=".repeat(60))
        println("📄 Rapor: build/reports/test-report.html")
        println("")
        println("📌 PDF olarak kaydetmek için:")
        println("   1. Raporu tarayıcıda açın")
        println("   2. Ctrl + P basın")
        println("   3. 'PDF olarak kaydet' seçin")
        println("=".repeat(60))
    }
}
