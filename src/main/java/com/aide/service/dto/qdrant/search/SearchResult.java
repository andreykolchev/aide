package com.aide.service.dto.qdrant.search;

public record SearchResult(Long chunkId, Long documentId, String project, Double score) {
}
