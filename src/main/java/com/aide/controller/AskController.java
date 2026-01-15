package com.aide.controller;

import com.aide.controller.dto.AskRequest;
import com.aide.controller.dto.AskResponse;
import com.aide.service.AskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ask")
@Tag(name = "Ask", description = "Ask questions about documentation")
public class AskController {

    private final AskService askService;

    public AskController(AskService askService) {
        this.askService = askService;
    }

    @PostMapping
    @Operation(summary = "Ask a question", description = "Search documentation and generate an AI-powered answer")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return askService.ask(request.question(), request.project());
    }
}
