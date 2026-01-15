package com.aide.service.dto.qdrant.search;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SearchRequest(
        List<Float> vector,
        int limit,
        @JsonProperty("with_payload") boolean withPayload,
        QdrantFilter filter
) {
}