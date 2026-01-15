package com.aide.service;

import com.aide.service.dto.IngestionResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IngestionServiceTest {

    @TempDir
    Path tempDir;

    private IngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new IngestionService(tempDir.resolve("docs").toString());
    }

    @Test
    void ingestPlainTextShouldReturnContentAndSaveFile() throws IOException {
        String content = "Hello text file";
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));

        IngestionResult result = ingestionService.ingest(file);

        Path savedFile = tempDir.resolve("docs").resolve("sample.txt");
        assertThat(result.text()).isEqualTo(content);
        assertThat(result.storedPath()).isEqualTo(savedFile);
        assertThat(Files.exists(savedFile)).isTrue();
        assertThat(Files.readString(savedFile)).isEqualTo(content);
    }

    @Test
    void ingestPdfShouldExtractText() throws IOException {
        byte[] pdfBytes = createPdf("Hello PDF content");
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.pdf", "application/pdf", pdfBytes);

        IngestionResult result = ingestionService.ingest(file);

        Path savedFile = tempDir.resolve("docs").resolve("sample.pdf");
        assertThat(result.text()).contains("Hello PDF content");
        assertThat(result.storedPath()).isEqualTo(savedFile);
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    void ingestUnsupportedFileShouldThrow() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.png", "image/png", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> ingestionService.ingest(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");
    }

    private byte[] createPdf(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(25, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                document.save(output);
                return output.toByteArray();
            }
        }
    }
}