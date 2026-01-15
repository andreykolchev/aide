package com.aide.service.dto.qdrant.embedding;

import java.util.List;

public record UpsertPointsRequest(List<PointStruct> points) {
}