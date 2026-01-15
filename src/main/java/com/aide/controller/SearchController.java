package com.aide.controller;

import com.aide.controller.dto.SearchContentRequest;
import com.aide.controller.dto.SearchContentResponse;
import com.aide.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Search documentation")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    @Operation(summary = "Search content", description = "Search documentation by query and project")
    public List<SearchContentResponse> searchContent(@Valid @RequestBody SearchContentRequest request) {
        return searchService.searchContent(request.query(), request.project());
    }
}