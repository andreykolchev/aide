package com.aide.service;

import com.aide.service.dto.IngestionResult;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;

@Service
public class IngestionService {

    private final Path storageDirectory;

    @Autowired
    public IngestionService(@Value("${app.ingestion.docs-path:./data/docs}") String storageDirectory) {
        this.storageDirectory = Paths.get(storageDirectory).toAbsolutePath().normalize();
    }

    public IngestionResult ingest(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be null or empty");
        }

        String fileName = Optional.ofNullable(file.getOriginalFilename()).map(Paths::get).map(Path::getFileName).map(Path::toString).orElseThrow(() -> new IllegalArgumentException("File name is required"));

        createStorageDirectory();

        Path destination = storageDirectory.resolve(fileName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save file: " + fileName, e);
        }

        String lowerCaseName = fileName.toLowerCase(Locale.ROOT);
        String text;
        try {
            if (isPdf(file, lowerCaseName)) {
                text = extractPdfText(destination);
            } else if (isText(file, lowerCaseName)) {
                text = Files.readString(destination);
            } else {
                throw new IllegalArgumentException("Unsupported file type: " + fileName);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to extract text from file: " + fileName, e);
        }

        return new IngestionResult(text, destination);
    }

    private void createStorageDirectory() {
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create storage directory: " + storageDirectory, e);
        }
    }

    private boolean isPdf(MultipartFile file, String lowerCaseName) {
        String contentType = file.getContentType();
        return lowerCaseName.endsWith(".pdf") || (contentType != null && contentType.equalsIgnoreCase("application/pdf"));
    }

    private boolean isText(MultipartFile file, String lowerCaseName) {
        String contentType = file.getContentType();
        return lowerCaseName.endsWith(".txt") || (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("text"));
    }

    private String extractPdfText(Path pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}