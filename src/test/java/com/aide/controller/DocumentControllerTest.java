package com.aide.controller;

import com.aide.controller.dto.UploadResponse;
import com.aide.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @Test
    void uploadDocumentShouldProcessAndReturnIds() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "sample.txt", "text/plain", "sample content".getBytes());

        when(documentService.uploadDocument(file, "demo"))
                .thenReturn(new UploadResponse(100L, 2));

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("project", "demo")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentId").value(100))
                .andExpect(jsonPath("$.chunkCount").value(2));

        verify(documentService).uploadDocument(file, "demo");
    }

    @Test
    void uploadDocumentShouldFailWhenFileEmpty() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "sample.txt", "text/plain", new byte[]{});

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("project", "demo")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentService);
    }

    @Test
    void downloadDocumentShouldReturnFileWithCorrectHeaders() throws Exception {
        Long documentId = 1L;
        String fileName = "sample.pdf";

        // Create a temporary file that persists for the test
        Path tempPath = Files.createTempFile("sample", ".pdf");
        try {
            Files.write(tempPath, "PDF content".getBytes());

            FileSystemResource mockResource = new FileSystemResource(tempPath.toFile());
            ResponseEntity<org.springframework.core.io.Resource> responseEntity = ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(mockResource);

            when(documentService.downloadDocument(documentId))
                    .thenReturn(responseEntity);

            mockMvc.perform(get("/api/documents/{id}", documentId))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\""));

            verify(documentService).downloadDocument(documentId);
        } finally {
            // Clean up the temporary file
            Files.deleteIfExists(tempPath);
        }
    }

    @Test
    void downloadDocumentShouldFailWhenDocumentNotFound() throws Exception {
        Long documentId = 999L;

        when(documentService.downloadDocument(documentId))
                .thenThrow(new IllegalArgumentException("Document not found with id: " + documentId));

        mockMvc.perform(get("/api/documents/{id}", documentId))
                .andExpect(status().isBadRequest());

        verify(documentService).downloadDocument(documentId);
    }
}