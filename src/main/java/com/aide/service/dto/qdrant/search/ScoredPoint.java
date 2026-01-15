package com.aide.service.dto.qdrant.search;

import com.aide.service.dto.qdrant.embedding.QdrantPayload;

public record ScoredPoint(Object id, Double score, QdrantPayload payload) {
}