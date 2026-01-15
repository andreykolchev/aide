package com.aide.service;

import com.aide.controller.dto.SearchContentResponse;
import com.aide.model.Document;
import com.aide.model.DocumentChunk;
import com.aide.repository.DocumentChunkRepository;
import com.aide.repository.DocumentRepository;
import com.aide.service.dto.qdrant.search.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SearchServiceTest {

    private EmbeddingService embeddingService;
    private QdrantService qdrantService;
    private DocumentChunkRepository documentChunkRepository;
    private DocumentRepository documentRepository;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        embeddingService = mock(EmbeddingService.class);
        qdrantService = mock(QdrantService.class);
        documentChunkRepository = mock(DocumentChunkRepository.class);
        documentRepository = mock(DocumentRepository.class);

        searchService = new SearchService(embeddingService, qdrantService, documentChunkRepository, documentRepository, 5);
    }

    @Test
    void searchContentShouldReturnRankedResults() {
        when(embeddingService.embed("hello world"))
                .thenReturn(List.of(0.1f, 0.2f, 0.3f));

        List<SearchResult> matches = List.of(
                new SearchResult(1L, 10L, "demo", 0.9d),
                new SearchResult(2L, 11L, "demo", 0.8d)
        );
        when(qdrantService.searchSimilar(any(), eq(5), eq("demo"))).thenReturn(matches);

        DocumentChunk chunk1 = new DocumentChunk();
        chunk1.setId(1L);
        chunk1.setDocumentId(10L);
        chunk1.setChunkIndex(0);
        chunk1.setContent("chunk-1");

        DocumentChunk chunk2 = new DocumentChunk();
        chunk2.setId(2L);
        chunk2.setDocumentId(11L);
        chunk2.setChunkIndex(1);
        chunk2.setContent("chunk-2");

        when(documentChunkRepository.findAllById(any())).thenReturn(List.of(chunk1, chunk2));

        Document doc1 = new Document();
        doc1.setId(10L);
        doc1.setFilePath("/path/to/doc1.txt");
        doc1.setName("doc1.txt");
        doc1.setProject("demo");
        doc1.setUploadedAt(Instant.now());

        Document doc2 = new Document();
        doc2.setId(11L);
        doc2.setFilePath("/path/to/doc2.txt");
        doc2.setName("doc2.txt");
        doc2.setProject("demo");
        doc2.setUploadedAt(Instant.now());

        when(documentRepository.findAllById(any())).thenReturn(List.of(doc1, doc2));

        List<SearchContentResponse> results = searchService.searchContent(" hello world ", " demo ");

        assertThat(results).hasSize(2);
        assertThat(results.get(0).documentId()).isEqualTo(10L);
        assertThat(results.get(0).documentName()).isEqualTo("doc1.txt");
        assertThat(results.get(0).documentPath()).isEqualTo("/path/to/doc1.txt");
        assertThat(results.get(0).chunkId()).isEqualTo(1L);
        assertThat(results.get(0).content()).isEqualTo("chunk-1");
        assertThat(results.get(0).score()).isEqualTo(0.9d);
        assertThat(results.get(1).documentId()).isEqualTo(11L);
        assertThat(results.get(1).documentName()).isEqualTo("doc2.txt");
        assertThat(results.get(1).documentPath()).isEqualTo("/path/to/doc2.txt");
        assertThat(results.get(1).chunkId()).isEqualTo(2L);
        assertThat(results.get(1).content()).isEqualTo("chunk-2");
        assertThat(results.get(1).score()).isEqualTo(0.8d);

        verify(embeddingService).embed("hello world");
        verify(qdrantService).searchSimilar(any(), eq(5), eq("demo"));
    }

    @Test
    void searchContentShouldSkipResultsWithoutChunks() {
        when(embeddingService.embed("hello"))
                .thenReturn(List.of(0.1f, 0.2f, 0.3f));

        List<SearchResult> matches = List.of(
                new SearchResult(5L, 20L, "demo", 0.7d)
        );
        when(qdrantService.searchSimilar(any(), eq(5), eq("demo"))).thenReturn(matches);

        when(documentChunkRepository.findAllById(any())).thenReturn(List.of());

        List<SearchContentResponse> results = searchService.searchContent("hello", "demo");

        assertThat(results).isEmpty();
    }

    @Test
    void searchContentShouldValidateInputs() {
        assertThatThrownBy(() -> searchService.searchContent("   ", "demo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("query must not be null or blank");

        assertThatThrownBy(() -> searchService.searchContent("hello", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("project must not be null or blank");
    }
}