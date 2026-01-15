package com.aide.controller;


import com.aide.controller.dto.AskRequest;
import com.aide.controller.dto.AskResponse;
import com.aide.service.AskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AskController.class)
class AskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AskService askService;

    @Test
    void shouldReturnAnswerFromAskService() throws Exception {
        AskRequest request = new AskRequest("How is the system architecture designed?", "project-a");
        AskResponse response = new AskResponse("The system consists of API, intelligence, and storage layers.");
        when(askService.ask(request.question(), request.project())).thenReturn(response);

        mockMvc.perform(post("/api/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("The system consists of API, intelligence, and storage layers."));
    }
}