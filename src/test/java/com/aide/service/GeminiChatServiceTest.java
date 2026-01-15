package com.aide.service;

import com.google.genai.Models;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GeminiChatServiceTest {

    @Test
    void shouldReturnAnswerFromGemini() {
        // given
        Models models = mock(Models.class);
        String modelName = "gemini-pro";
        String prompt = "Explain the system architecture.";

        GenerateContentResponse response = mock(GenerateContentResponse.class);
        when(response.text()).thenReturn("The system uses a layered architecture.");

        when(models.generateContent(
                eq(modelName),
                eq(prompt),
                any(GenerateContentConfig.class)
        )).thenReturn(response);

        GeminiChatService service = new GeminiChatService(models, modelName);

        // when
        String result = service.generateAnswer(prompt);

        // then
        assertThat(result).isEqualTo("The system uses a layered architecture.");
    }

    @Test
    void shouldThrowExceptionWhenPromptIsNull() {
        GeminiChatService service = new GeminiChatService(mock(Models.class), "gemini-pro");

        assertThatThrownBy(() -> service.generateAnswer(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt must not be null or blank");
    }

    @Test
    void shouldThrowExceptionWhenPromptIsBlank() {
        GeminiChatService service = new GeminiChatService(mock(Models.class), "gemini-pro");

        assertThatThrownBy(() -> service.generateAnswer("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt must not be null or blank");
    }

    @Test
    void shouldThrowExceptionWhenGeminiReturnsEmptyResponse() {
        // given
        Models models = mock(Models.class);
        GenerateContentResponse response = mock(GenerateContentResponse.class);

        when(response.text()).thenReturn(null);
        when(models.generateContent(anyString(), anyString(), any(GenerateContentConfig.class))).thenReturn(response);

        GeminiChatService service = new GeminiChatService(models, "gemini-pro");

        // when / then
        assertThatThrownBy(() -> service.generateAnswer("test prompt"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Empty response from Gemini");
    }
}