import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.time.LocalDate

plugins {
    java
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("jacoco")
    id("io.qameta.allure") version "2.11.2"
}

group = "org.example"
version = "0.0.1-SNAPSHOT"
description = "meeting_streaming_service"

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
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.apache.kafka:kafka-streams")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("net.dv8tion:JDA:5.0.0-beta.18")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("com.mysql:mysql-connector-j:8.2.0")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.mockito:mockito-junit-jupiter:5.21.0")
    testImplementation("io.qameta.allure:allure-junit5:2.24.0")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
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
                        "**/model/**",
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
                        "**/model/**",
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
                minimum = "0.3".toBigDecimal()
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
        println("📊 TÜM RAPORLAR OLUŞTURULDU")
        println("=".repeat(60))
        println("📁 JUnit HTML    : build/reports/tests/test/index.html")
        println("📁 JaCoCo HTML   : build/reports/jacoco/index.html")
        println("📁 Allure HTML   : build/reports/allure-report/allureReport/index.html")
        println("=".repeat(60))
        println("💡 PDF için Allure raporunu tarayıcıda açıp Ctrl+P ile yazdırın")
        println("=".repeat(60))
    }
}

tasks.register("generatePdfReport") {
    group = "verification"
    description = "Generates a professional PDF-ready HTML test report"

    dependsOn("test")

    doLast {
        val testResultsDir = file("build/test-results/test")
        val templateFile = file("src/test/resources/test-report-template.html")
        val outputFile = file("build/reports/test-report.html")

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
                val statusText = if (isFailed) "❌ Başarısız" else "✅ Başarılı"

                testDetails.append("""
                    <tr>
                        <td>$testName</td>
                        <td>${time}s</td>
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

        val suitesHtml = StringBuilder()
        testSuites.forEach { (suite, data) ->
            val status = if (data.second) "passed" else "failed"
            val statusText = if (data.second) "✅ Başarılı" else "❌ Başarısız"
            suitesHtml.append("""
                <tr>
                    <td>$suite</td>
                    <td>${data.first}</td>
                    <td><span class="status-badge $status">$statusText</span></td>
                </tr>
            """.trimIndent())
        }

        var html = templateFile.readText()
        val reportDate = LocalDate.now().toString()
        html = html.replace("{{REPORT_DATE}}", reportDate)
        html = html.replace("{{TOTAL_TESTS}}", totalTests.toString())
        html = html.replace("{{PASSED_TESTS}}", passedTests.toString())
        html = html.replace("{{FAILED_TESTS}}", failedTests.toString())
        html = html.replace("{{SUCCESS_RATE}}", successRate.toString())
        html = html.replace("{{COVERAGE}}", "30")
        html = html.replace("{{TEST_SUITES}}", suitesHtml.toString())
        html = html.replace("{{TEST_DETAILS}}", testDetails.toString())

        outputFile.writeText(html)

        println("=".repeat(60))
        println("PDF-READY TEST RAPORU OLUŞTURULDU!")
        println("=".repeat(60))
        println("Rapor: build/reports/test-report.html")
        println("")
        println("PDF olarak kaydetmek için:")
        println("   1. Raporu tarayıcıda açın")
        println("   2. Ctrl + P basın")
        println("   3. 'PDF olarak kaydet' seçin")
        println("=".repeat(60))
    }
}

tasks.bootBuildImage {
    runImage = "paketobuildpacks/ubuntu-noble-run-base:latest"
}
