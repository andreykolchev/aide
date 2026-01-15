package com.aide.service;

import com.aide.controller.dto.SearchContentResponse;
import com.aide.model.Document;
import com.aide.model.DocumentChunk;
import com.aide.repository.DocumentChunkRepository;
import com.aide.repository.DocumentRepository;
import com.aide.service.dto.qdrant.search.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;
    private final int searchLimit;

    @Autowired
    public SearchService(
            EmbeddingService embeddingService,
            QdrantService qdrantService,
            DocumentChunkRepository documentChunkRepository,
            DocumentRepository documentRepository,
            @Value("${app.search.default-limit:5}") int searchLimit
    ) {
        this.embeddingService = embeddingService;
        this.qdrantService = qdrantService;
        this.documentChunkRepository = documentChunkRepository;
        this.documentRepository = documentRepository;
        this.searchLimit = searchLimit;
    }

    @Transactional(readOnly = true)
    public List<SearchContentResponse> searchContent(String query, String project) {
        String normalizedQuery = normalizeQuery(query);
        String normalizedProject = normalizeProject(project);

        List<Float> embedding = embeddingService.embed(normalizedQuery);
        List<SearchResult> matches = qdrantService.searchSimilar(embedding, searchLimit, normalizedProject);
        if (matches.isEmpty()) {
            return List.of();
        }

        Map<Long, DocumentChunk> chunksById = documentChunkRepository.findAllById(
                        matches.stream().map(SearchResult::chunkId).toList()
                ).stream()
                .collect(Collectors.toMap(DocumentChunk::getId, chunk -> chunk));

        // Fetch all documents for the chunks
        List<Long> documentIds = chunksById.values().stream()
                .map(DocumentChunk::getDocumentId)
                .distinct()
                .toList();
        Map<Long, Document> documentsById = documentRepository.findAllById(documentIds).stream()
                .collect(Collectors.toMap(Document::getId, doc -> doc));

        return matches.stream()
                .map(match -> toSearchResult(match, chunksById.get(match.chunkId()), documentsById))
                .filter(Objects::nonNull)
                .toList();
    }

    private SearchContentResponse toSearchResult(SearchResult match, DocumentChunk chunk, Map<Long, Document> documentsById) {
        if (chunk == null) {
            return null;
        }
        Document document = documentsById.get(chunk.getDocumentId());
        if (document == null) {
            return null;
        }
        return new SearchContentResponse(
                document.getId(),
                document.getName(),
                document.getFilePath(),
                match.chunkId(),
                chunk.getContent(),
                match.score()
        );
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            throw new IllegalArgumentException("query must not be null or blank");
        }
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("query must not be null or blank");
        }
        return trimmed;
    }

    private String normalizeProject(String project) {
        if (project == null) {
            throw new IllegalArgumentException("project must not be null or blank");
        }
        String trimmed = project.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("project must not be null or blank");
        }
        return trimmed;
    }
}