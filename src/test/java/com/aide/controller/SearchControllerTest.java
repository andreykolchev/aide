package com.aide.controller;

import com.aide.controller.dto.SearchContentRequest;
import com.aide.controller.dto.SearchContentResponse;
import com.aide.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    @Test
    void searchShouldReturnResults() throws Exception {
        SearchContentRequest request = new SearchContentRequest("hello", "demo");
        List<SearchContentResponse> results = List.of(
                new SearchContentResponse(1L, "doc1.txt", "/path/to/doc1.txt", 1L, "chunk-1", 0.9d),
                new SearchContentResponse(2L, "doc2.txt", "/path/to/doc2.txt", 2L, "chunk-2", 0.8d)
        );
        when(searchService.searchContent("hello", "demo")).thenReturn(results);

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].documentPath", is("/path/to/doc1.txt")))
                .andExpect(jsonPath("$[0].documentName", is("doc1.txt")))
                .andExpect(jsonPath("$[0].chunkId", is(1)))
                .andExpect(jsonPath("$[0].content", is("chunk-1")))
                .andExpect(jsonPath("$[0].score", is(0.9)))
                .andExpect(jsonPath("$[1].chunkId", is(2)));
    }

    @Test
    void searchShouldReturnBadRequestOnValidationError() throws Exception {
        SearchContentRequest request = new SearchContentRequest(" ", "demo");
        when(searchService.searchContent(" ", "demo"))
                .thenThrow(new IllegalArgumentException("query must not be null or blank"));

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}