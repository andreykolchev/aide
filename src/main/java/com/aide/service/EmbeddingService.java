package com.aide.service;

import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.errors.ApiException;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class EmbeddingService {

    private final Models models;
    private final String model;

    @Autowired
    public EmbeddingService(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.embeddings.model}") String model
    ) {
        this(Client.builder().apiKey(apiKey).build().models, model);
    }

    EmbeddingService(Models models, String model) {
        if (models == null) {
            throw new IllegalStateException("Gemini client is not configured");
        }
        this.models = models;
        this.model = model;
    }

    public List<Float> embed(String text) {
        if (text == null || text.isBlank()) {
            log.error("Text to embed must not be null or blank");
            throw new IllegalArgumentException("Text must not be null or blank");
        }
        EmbedContentResponse response;
        try {
            response = models.embedContent(model, text, EmbedContentConfig.builder().build());
        } catch (ApiException e) {
            String message = Optional.ofNullable(e.message())
                    .filter(msg -> !msg.isBlank())
                    .orElse("status: " + e.status());
            log.error("Gemini embeddings request failed: {}", message, e);
            throw new IllegalStateException("Gemini embeddings request failed: " + message, e);
        } catch (Exception e) {
            log.error("Unexpected error calling Gemini embeddings API", e);
            throw new IllegalStateException("Failed to call Gemini embeddings API", e);
        }

        List<Float> embedding = response.embeddings()
                .flatMap(list -> list.stream().findFirst())
                .flatMap(ContentEmbedding::values)
                .orElseThrow(() -> new IllegalStateException("Empty embedding response from Gemini"));

        if (embedding.isEmpty()) {
            log.error("Empty embedding response from Gemini");
            throw new IllegalStateException("Empty embedding response from Gemini");
        }

        return embedding;
    }
}
