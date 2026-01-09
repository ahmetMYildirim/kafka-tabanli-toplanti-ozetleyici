import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.time.LocalDate

plugins {
    java
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
}

group = "org.example"
version = "1.0.0"
description = "ai_service"

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
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.kafka:spring-kafka")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    runtimeOnly("com.mysql:mysql-connector-j:8.2.0")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("org.springframework.boot:spring-boot-starter-validation")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("com.openhtmltopdf:openhtmltopdf-core:1.0.10")
    testImplementation("com.openhtmltopdf:openhtmltopdf-pdfbox:1.0.10")
}


tasks.withType<Test> {
    useJUnitPlatform()
}


tasks.register<Test>("testForReport") {
    group = "verification"
    description = "Runs tests but does not fail the build (for report generation)."
    useJUnitPlatform()
    ignoreFailures = true
}

tasks.withType<JacocoReport> {
    dependsOn("testForReport")

    reports {
        xml.required.set(true)
        csv.required.set(true)
        html.required.set(true)

        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco"))
    }
}

tasks.withType<JacocoCoverageVerification> {
    dependsOn("testForReport")

    violationRules {
        rule {
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal() // %90
            }
        }
    }
}

tasks.register("testReport") {
    group = "verification"
    description = "Generates test + JaCoCo reports"

    dependsOn("testForReport", "jacocoTestReport")

    doLast {
        println("=".repeat(60))
        println("RAPORLAR OLUŞTURULDU")
        println("=".repeat(60))
        println(" JUnit HTML : build/reports/tests/test/index.html")
        println(" JaCoCo HTML: build/reports/jacoco/index.html")
        println(" JaCoCo XML : build/reports/jacoco/jacocoTestReport.xml")
        println(" JaCoCo CSV : build/reports/jacoco/jacocoTestReport.csv")
        println("=".repeat(60))
    }
}

tasks.register("generatePdfReport") {
    group = "verification"
    description = "Generates a professional PDF-ready HTML test report from template"

    dependsOn("testForReport", "jacocoTestReport")

    doLast {
        val testResultsDir = file("build/test-results/test")
        val templateFile = file("src/test/resources/test-report-template.html")
        val outputFile = file("build/reports/test-report.html")

        val jacocoCsvFile = file("build/reports/jacoco/jacocoTestReport.csv")

        require(templateFile.exists()) {
            "Template bulunamadı: ${templateFile.path}\n" +
                    "Template'i buraya koy: src/test/resources/test-report-template.html"
        }

        var totalTests = 0
        var failedTests = 0
        val testDetails = StringBuilder()
        val testSuites = mutableMapOf<String, Pair<Int, Boolean>>() // suite -> (count, allPassed)

        testResultsDir.listFiles()
            ?.filter { it.extension == "xml" }
            ?.forEach { xmlFile ->
                val content = xmlFile.readText()

                val tests = Regex("tests=\"(\\d+)\"").find(content)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val failures = Regex("failures=\"(\\d+)\"").find(content)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val errors = Regex("errors=\"(\\d+)\"").find(content)?.groupValues?.get(1)?.toIntOrNull() ?: 0

                totalTests += tests
                failedTests += failures + errors

                val testCaseRegex =
                    Regex("<testcase name=\"([^\"]+)\"[^>]*classname=\"([^\"]+)\"[^>]*time=\"([^\"]+)\"[^>]*(/?>)")

                testCaseRegex.findAll(content).forEach { match ->
                    val testName = match.groupValues[1]
                    val className = match.groupValues[2].substringAfterLast(".")
                    val time = match.groupValues[3]

                    val fileHasFailure = (failures + errors) > 0

                    val status = if (fileHasFailure) "failed" else "passed"
                    val statusText = if (fileHasFailure) "Başarısız" else "Başarılı"

                    testDetails.append(
                        """
                        <tr>
                          <td>$testName</td>
                          <td><span class="duration">${time}s</span></td>
                          <td><span class="status-badge $status">$statusText</span></td>
                        </tr>
                        """.trimIndent()
                    )

                    val current = testSuites[className] ?: Pair(0, true)
                    testSuites[className] = Pair(current.first + 1, current.second && !fileHasFailure)
                }
            }

        val passedTests = totalTests - failedTests
        val successRate = if (totalTests > 0) (passedTests * 100 / totalTests) else 0

        var coverage = 0
        if (jacocoCsvFile.exists()) {
            val lines = jacocoCsvFile.readLines()
            if (lines.size > 1) {
                var totalInstructions = 0L
                var coveredInstructions = 0L

                lines.drop(1).forEach { line ->
                    val cols = line.split(",")
                    if (cols.size >= 5) {
                        val missed = cols[3].toLongOrNull() ?: 0L
                        val covered = cols[4].toLongOrNull() ?: 0L
                        totalInstructions += (missed + covered)
                        coveredInstructions += covered
                    }
                }
                if (totalInstructions > 0) {
                    coverage = ((coveredInstructions * 100) / totalInstructions).toInt()
                }
            }
        } else {
            println("JaCoCo CSV bulunamadı: ${jacocoCsvFile.path}")
        }

        val suitesHtml = StringBuilder()
        testSuites.toSortedMap().forEach { (suite, data) ->
            val ok = data.second
            val status = if (ok) "passed" else "failed"
            val statusText = if (ok) "Başarılı" else "Başarısız"

            suitesHtml.append(
                """
                <tr>
                  <td>$suite</td>
                  <td>${data.first}</td>
                  <td><span class="status-badge $status">$statusText</span></td>
                </tr>
                """.trimIndent()
            )
        }

        var html = templateFile.readText()

        val reportDate = LocalDate.now().toString()
        html = html.replace("{{REPORT_DATE}}", reportDate)
        html = html.replace("{{TOTAL_TESTS}}", totalTests.toString())
        html = html.replace("{{PASSED_TESTS}}", passedTests.toString())
        html = html.replace("{{FAILED_TESTS}}", failedTests.toString())
        html = html.replace("{{SUCCESS_RATE}}", successRate.toString())
        html = html.replace("{{COVERAGE}}", coverage.toString())
        html = html.replace("{{TEST_SUITES}}", if (suitesHtml.isNotBlank()) suitesHtml.toString() else "<tr><td colspan='3'>Suite yok</td></tr>")
        html = html.replace("{{TEST_DETAILS}}", if (testDetails.isNotBlank()) testDetails.toString() else "<tr><td colspan='3'>Detay yok</td></tr>")

        outputFile.parentFile.mkdirs()
        outputFile.writeText(html)

        println("=".repeat(60))
        println("PDF-READY HTML RAPOR OLUŞTURULDU")
        println("=".repeat(60))
        println("Rapor: build/reports/test-report.html")
        println("")
        println("PDF olarak almak için:")
        println("  1) Raporu tarayıcıda aç")
        println("  2) Ctrl + P")
        println("  3) PDF olarak kaydet")
        println("=".repeat(60))
    }
}
