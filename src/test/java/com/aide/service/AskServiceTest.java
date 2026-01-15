package com.aide.service;

import com.aide.controller.dto.AskResponse;
import com.aide.controller.dto.SearchContentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AskServiceTest {

    private SearchService searchService;
    private GeminiChatService geminiChatService;
    private AskService askService;

    @BeforeEach
    void setUp() {
        searchService = mock(SearchService.class);
        geminiChatService = mock(GeminiChatService.class);
        askService = new AskService(searchService, geminiChatService);
    }

    @Test
    void shouldBuildGroundedPromptAndReturnSources() {
        // given
        String question = "How is the system architecture designed?";
        String project = "project-test";
        List<SearchContentResponse> searchResults = List.of(
                new SearchContentResponse(1L, "doc1.txt", "/path/to/doc1.txt", 1L, "The system consists of an API layer.", 0.7D),
                new SearchContentResponse(2L, "doc1.txt", "/path/to/doc2.txt", 2L, "The backend is implemented using Spring Boot.", 0.8D)
        );
        when(searchService.searchContent(question, project)).thenReturn(searchResults);
        when(geminiChatService.generateAnswer(anyString())).thenReturn("The system architecture consists of an API layer and a Spring Boot backend.");

        // when
        AskResponse response = askService.ask(question, project);

        // then
        assertThat(response.answer())
                .isEqualTo("The system architecture consists of an API layer and a Spring Boot backend.");


        // Capture prompt passed to Gemini
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiChatService).generateAnswer(promptCaptor.capture());

        String prompt = promptCaptor.getValue();

        // Prompt grounding assertions
        assertThat(prompt)
                .contains("ONLY the provided context")
                .contains("[Chunk 1]")
                .contains("The system consists of an API layer.")
                .contains("[Chunk 2]")
                .contains("The backend is implemented using Spring Boot.")
                .contains(question);

        // Verify search invocation
        verify(searchService).searchContent(question, project);
        verifyNoMoreInteractions(searchService, geminiChatService);
    }
}