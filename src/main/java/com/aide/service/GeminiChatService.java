package com.aide.service;

import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GeminiChatService {

    private final Models models;
    private final String model;

    @Autowired
    public GeminiChatService(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.chat.model}") String model
    ) {
        this(Client.builder().apiKey(apiKey).build().models, model);
    }

    GeminiChatService(Models models, String model) {
        this.models = models;
        this.model = model;
    }

    public String generateAnswer(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt must not be null or blank");
        }
        GenerateContentResponse geminiResp = models.generateContent(model, prompt, GenerateContentConfig.builder().build());
        return Optional.ofNullable(geminiResp.text()).orElseThrow(() -> new IllegalStateException("Empty response from Gemini."));
    }
}
