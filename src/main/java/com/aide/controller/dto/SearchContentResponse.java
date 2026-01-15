package com.aide.controller.dto;

public record SearchContentResponse(
        Long documentId, String documentName, String documentPath, Long chunkId, String content, Double score) {
}