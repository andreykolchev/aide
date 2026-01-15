package com.aide.service.dto.qdrant.embedding;

public record QdrantPayload(Long chunkId, Long documentId, String project) {
}