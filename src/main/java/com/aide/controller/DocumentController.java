package com.aide.controller;

import com.aide.controller.dto.UploadResponse;
import com.aide.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Document management")
public class DocumentController {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50MB

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload document", description = "Upload a PDF or text document for processing")
    public UploadResponse uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "project") @NotBlank String project
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be null or empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit" + (MAX_FILE_SIZE / (1024 * 1024)) + "MB");
        }
        return documentService.uploadDocument(file, project);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Download document", description = "Download a previously uploaded document")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        return documentService.downloadDocument(id);
    }
}