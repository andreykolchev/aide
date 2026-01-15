package com.aide.service.dto.qdrant.search;

import java.util.List;

public record SearchResponse(List<ScoredPoint> result) {
}