package org.example.ai_service.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * PDF Test Raporu Oluşturucu
 * HTML template'i kullanarak test sonuçlarını PDF'e çevirir.
 */
public class TestReportPdfGenerator {

    private static final String TEMPLATE_PATH = "src/test/resources/test-report-template.html";
    private static final String OUTPUT_DIR = "build/reports/tests/pdf/";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /**
     * HTML test raporunu PDF'e çevirir
     *
     * @param htmlFilePath HTML rapor dosyasının yolu
     * @return Oluşturulan PDF dosyasının yolu
     */
    public static String generatePdfFromHtml(String htmlFilePath) {
        try {
            
            Files.createDirectories(Paths.get(OUTPUT_DIR));

            
            String htmlContent = readHtmlFile(htmlFilePath);

            
            String xhtmlContent = convertToXhtml(htmlContent);

            
            String timestamp = LocalDateTime.now().format(FORMATTER);
            String pdfFileName = OUTPUT_DIR + "test-report-" + timestamp + ".pdf";

            
            try (OutputStream os = new FileOutputStream(pdfFileName)) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(xhtmlContent, new File(".").toURI().toString());
                builder.toStream(os);
                builder.run();
            }

            System.out.println("✓ PDF rapor oluşturuldu: " + pdfFileName);
            return pdfFileName;

        } catch (Exception e) {
            System.err.println("✗ PDF oluşturma hatası: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Template'i kullanarak özel HTML raporu oluşturur
     *
     * @param testStats Test istatistikleri
     * @return Oluşturulan HTML dosyasının yolu
     */
    public static String generateCustomHtmlReport(TestStats testStats) {
        try {
            
            String template = readHtmlFile(TEMPLATE_PATH);

            
            String htmlContent = template
                .replace("{{TOTAL_TESTS}}", String.valueOf(testStats.getTotalTests()))
                .replace("{{PASSED_TESTS}}", String.valueOf(testStats.getPassedTests()))
                .replace("{{FAILED_TESTS}}", String.valueOf(testStats.getFailedTests()))
                .replace("{{COVERAGE}}", String.format("%.1f%%", testStats.getCoverage()))
                .replace("{{DATE}}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
                .replace("{{DURATION}}", String.format("%.2f sn", testStats.getDuration()))
                .replace("{{TEST_DETAILS}}", generateTestDetailsHtml(testStats));

            
            String timestamp = LocalDateTime.now().format(FORMATTER);
            String htmlFileName = OUTPUT_DIR + "test-report-" + timestamp + ".html";
            Files.createDirectories(Paths.get(OUTPUT_DIR));
            Files.write(Paths.get(htmlFileName), htmlContent.getBytes(StandardCharsets.UTF_8));

            System.out.println("✓ HTML rapor oluşturuldu: " + htmlFileName);
            return htmlFileName;

        } catch (IOException e) {
            System.err.println("✗ HTML oluşturma hatası: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Test detaylarını HTML formatında oluşturur
     */
    private static String generateTestDetailsHtml(TestStats testStats) {
        StringBuilder html = new StringBuilder();

        for (TestClassResult testClass : testStats.getTestClasses()) {
            String statusClass = testClass.isPassed() ? "passed" : "failed";
            String icon = testClass.isPassed() ? "✓" : "✗";

            html.append(String.format(
                "<div class='test-class %s'>\n" +
                "  <div class='test-class-header'>\n" +
                "    <span class='icon'>%s</span>\n" +
                "    <span class='name'>%s</span>\n" +
                "    <span class='count'>%d test</span>\n" +
                "  </div>\n" +
                "</div>\n",
                statusClass, icon, testClass.getClassName(), testClass.getTestCount()
            ));
        }

        return html.toString();
    }

    /**
     * HTML dosyasını okur
     */
    private static String readHtmlFile(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
    }

    /**
     * HTML'i XHTML formatına çevirir (PDF uyumlu)
     */
    private static String convertToXhtml(String html) {
        try {
            
            html = html.replaceAll("<br>", "<br/>");
            html = html.replaceAll("<hr>", "<hr/>");
            html = html.replaceAll("<img ([^>]+)(?<!/)>", "<img $1/>");
            html = html.replaceAll("<meta ([^>]+)(?<!/)>", "<meta $1/>");

            
            if (!html.contains("<!DOCTYPE")) {
                html = "<!DOCTYPE html>\n" + html;
            }

            return html;
        } catch (Exception e) {
            return html; 
        }
    }

    /**
     * Test istatistikleri sınıfı
     */
    public static class TestStats {
        private int totalTests;
        private int passedTests;
        private int failedTests;
        private double coverage;
        private double duration;
        private java.util.List<TestClassResult> testClasses;

        public TestStats(int totalTests, int passedTests, int failedTests,
                        double coverage, double duration) {
            this.totalTests = totalTests;
            this.passedTests = passedTests;
            this.failedTests = failedTests;
            this.coverage = coverage;
            this.duration = duration;
            this.testClasses = new java.util.ArrayList<>();
        }

        public void addTestClass(String className, int testCount, boolean passed) {
            testClasses.add(new TestClassResult(className, testCount, passed));
        }

        
        public int getTotalTests() { return totalTests; }
        public int getPassedTests() { return passedTests; }
        public int getFailedTests() { return failedTests; }
        public double getCoverage() { return coverage; }
        public double getDuration() { return duration; }
        public java.util.List<TestClassResult> getTestClasses() { return testClasses; }
    }

    /**
     * Test sınıfı sonuç bilgisi
     */
    public static class TestClassResult {
        private String className;
        private int testCount;
        private boolean passed;

        public TestClassResult(String className, int testCount, boolean passed) {
            this.className = className;
            this.testCount = testCount;
            this.passed = passed;
        }

        public String getClassName() { return className; }
        public int getTestCount() { return testCount; }
        public boolean isPassed() { return passed; }
    }

    /**
     * Main metod - Test için
     */
    public static void main(String[] args) {
        System.out.println("=== PDF Test Raporu Oluşturucu ===\n");

        
        TestStats stats = new TestStats(75, 73, 2, 78.5, 45.3);
        stats.addTestClass("OpenAIClientTest", 12, true);
        stats.addTestClass("AudioCompressorTest", 8, true);
        stats.addTestClass("AudioAnalysisControllerTest", 15, true);
        stats.addTestClass("TranscriptionServiceTest", 10, true);
        stats.addTestClass("MeetingSummaryServiceTest", 10, true);
        stats.addTestClass("TaskExtractionServiceTest", 10, true);
        stats.addTestClass("AudioProcessingOrchestratorTest", 10, false);

        
        String htmlFile = generateCustomHtmlReport(stats);

        if (htmlFile != null) {
            
            String pdfFile = generatePdfFromHtml(htmlFile);

            if (pdfFile != null) {
                System.out.println("\n✓ Test raporu başarıyla oluşturuldu!");
                System.out.println("  HTML: " + htmlFile);
                System.out.println("  PDF:  " + pdfFile);
            }
        }
    }
}

