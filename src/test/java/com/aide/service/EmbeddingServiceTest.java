package com.aide.service;

import com.google.genai.Models;
import com.google.genai.errors.ApiException;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EmbeddingServiceTest {

    private Models models;
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        models = mock(Models.class);
        embeddingService = new EmbeddingService(models, "gemini-embedding-001");
    }

    @Test
    void embedShouldReturnEmbeddingOnSuccess() {
        ContentEmbedding embedding = ContentEmbedding.builder()
                .values(List.of(0.1f, 0.2f, 0.3f))
                .build();
        EmbedContentResponse response = EmbedContentResponse.builder()
                .embeddings(List.of(embedding))
                .build();

        when(models.embedContent(eq("gemini-embedding-001"), eq("hello world"), any(EmbedContentConfig.class)))
                .thenReturn(response);

        List<Float> result = embeddingService.embed("hello world");

        assertThat(result).containsExactly(0.1f, 0.2f, 0.3f);
        verify(models).embedContent(eq("gemini-embedding-001"), eq("hello world"), any(EmbedContentConfig.class));
    }

    @Test
    void embedShouldThrowWhenTextIsBlank() {
        assertThatThrownBy(() -> embeddingService.embed("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or blank");
    }

    @Test
    void embedShouldPropagateApiErrors() {
        when(models.embedContent(eq("gemini-embedding-001"), eq("hello"), any(EmbedContentConfig.class)))
                .thenThrow(new ApiException(400, "BAD_REQUEST", "bad"));

        assertThatThrownBy(() -> embeddingService.embed("hello"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bad");
    }
}
