package com.aide.service;

import com.aide.controller.dto.UploadResponse;
import com.aide.model.Document;
import com.aide.model.DocumentChunk;
import com.aide.repository.DocumentChunkRepository;
import com.aide.repository.DocumentRepository;
import com.aide.service.dto.IngestionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    private IngestionService ingestionService;
    private ChunkingService chunkingService;
    private EmbeddingService embeddingService;
    private QdrantService qdrantService;
    private DocumentRepository documentRepository;
    private DocumentChunkRepository documentChunkRepository;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        chunkingService = mock(ChunkingService.class);
        embeddingService = mock(EmbeddingService.class);
        qdrantService = mock(QdrantService.class);
        documentRepository = mock(DocumentRepository.class);
        documentChunkRepository = mock(DocumentChunkRepository.class);
        ingestionService = mock(IngestionService.class);

        documentService = new DocumentService(
                ingestionService,
                chunkingService,
                embeddingService,
                qdrantService,
                documentRepository,
                documentChunkRepository
        );
    }

    @Test
    void uploadDocumentShouldProcessAndReturnIds() {
        MockMultipartFile file = new MockMultipartFile("file", "sample.txt", "text/plain", "sample content".getBytes());

        when(ingestionService.ingest(file)).thenReturn(new IngestionResult("sample content", Path.of("sample.txt")));
        when(chunkingService.chunk("sample content")).thenReturn(List.of("chunk-1", "chunk-2"));
        when(documentRepository.save(any())).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.setId(100L);
            return document;
        });
        when(documentChunkRepository.saveAll(any())).thenAnswer(invocation -> {
            List<DocumentChunk> chunks = new ArrayList<>(invocation.getArgument(0));
            long id = 1;
            for (DocumentChunk chunk : chunks) {
                chunk.setId(id++);
            }
            return chunks;
        });
        when(embeddingService.embed(anyString())).thenReturn(List.of(0.1f, 0.2f));

        UploadResponse response = documentService.uploadDocument(file, " demo ");

        assertThat(response.documentId()).isEqualTo(100L);
        assertThat(response.chunkCount()).isEqualTo(2);

        verify(chunkingService).chunk("sample content");
        verify(documentChunkRepository).saveAll(any());
        verify(embeddingService, times(2)).embed(anyString());
        verify(qdrantService, times(2)).storeEmbedding(any(), eq(100L), eq("demo"), any());

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(documentCaptor.capture());
        assertThat(documentCaptor.getValue().getProject()).isEqualTo("demo");
        assertThat(documentCaptor.getValue().getFilePath()).endsWith("sample.txt");
    }

    @Test
    void uploadDocumentShouldHandleEmptyChunks() {
        MockMultipartFile file = new MockMultipartFile("file", "sample.txt", "text/plain", "   ".getBytes());

        when(ingestionService.ingest(file)).thenReturn(new IngestionResult("   ", Path.of("sample.txt")));
        when(chunkingService.chunk("   ")).thenReturn(List.of());
        when(documentRepository.save(any())).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.setId(55L);
            return document;
        });

        UploadResponse response = documentService.uploadDocument(file, "demo");

        assertThat(response.documentId()).isEqualTo(55L);
        assertThat(response.chunkCount()).isZero();

        verify(documentChunkRepository, never()).saveAll(any());
        verify(embeddingService, never()).embed(anyString());
        verify(qdrantService, never()).storeEmbedding(any(), any(), anyString(), any());
    }
}