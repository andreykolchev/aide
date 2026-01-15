package com.aide.service.dto.qdrant.embedding;

import java.util.List;

public record PointStruct(Object id, List<Float> vector, QdrantPayload payload) {
}