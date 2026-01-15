package com.aide.service.dto.qdrant.search;

import java.util.List;

public record QdrantFilter(List<QdrantCondition> must) {
}