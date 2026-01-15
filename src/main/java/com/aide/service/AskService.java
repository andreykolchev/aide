package com.aide.service;

import com.aide.controller.dto.AskResponse;
import com.aide.controller.dto.SearchContentResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AskService {

    private final SearchService searchService;
    private final GeminiChatService geminiChatService;

    public AskService(
            SearchService searchService,
            GeminiChatService geminiChatService
    ) {
        this.searchService = searchService;
        this.geminiChatService = geminiChatService;
    }

    public AskResponse ask(String question, String project) {
        // Finds relevant documentation fragments
        List<SearchContentResponse> searchResult = searchService.searchContent(question, project);

        if (searchResult.isEmpty()) {
            return new AskResponse("No relevant documentation found for your query.");
        }

        // Build grounded context
        String context = buildContext(searchResult);
        // Build strict prompt
        String prompt = buildPrompt(context, question);
        // Call Gemini
        String answer = geminiChatService.generateAnswer(prompt);
        return new AskResponse(answer);
    }

    private String buildContext(List<SearchContentResponse> searchResult) {
        return searchResult.stream()
                .map(c -> "[Chunk " + c.chunkId() + "]\n" + c.content())
                .collect(Collectors.joining("\n\n"));
    }

    private String buildPrompt(String context, String question) {
        return """
                You are answering a question using ONLY the provided context.
                If the answer cannot be found in the context, respond with:
                "The requested information is not available in the documentation."
                
                Context:
                %s
                
                Question:
                %s
                """.formatted(context, question);
    }
}

