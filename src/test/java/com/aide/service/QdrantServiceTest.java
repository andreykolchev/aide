package com.aide.service;

import com.aide.service.dto.qdrant.QdrantConstants;
import com.aide.service.dto.qdrant.search.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QdrantServiceTest {

    private static final String BASE_URL = "http://localhost:6333";
    private static final String COLLECTION = "aide";
    private static final double APP_SEARCH_SCORE_THRESHOLD = 0.65D;

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private QdrantService qdrantService;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        qdrantService = new QdrantService(restTemplate, BASE_URL, COLLECTION, APP_SEARCH_SCORE_THRESHOLD);
    }

    @Test
    void storeEmbeddingShouldCreateCollectionAndUpsert() {
        List<Float> vector = vectorWithLength(QdrantConstants.VECTOR_SIZE);

        server.expect(once(), requestTo(BASE_URL + "/collections/" + COLLECTION))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        server.expect(once(), requestTo(BASE_URL + "/collections/" + COLLECTION))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.vectors.size", is(QdrantConstants.VECTOR_SIZE)))
                .andExpect(jsonPath("$.vectors.distance", is(QdrantConstants.DISTANCE_COSINE)))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo(BASE_URL + "/collections/" + COLLECTION + "/points"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.points", hasSize(1)))
                .andExpect(jsonPath("$.points[0].payload.chunkId", is(1)))
                .andExpect(jsonPath("$.points[0].payload.documentId", is(10)))
                .andExpect(jsonPath("$.points[0].payload.project", is("demo")))
                .andExpect(jsonPath("$.points[0].vector", hasSize(QdrantConstants.VECTOR_SIZE)))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        qdrantService.storeEmbedding(1L, 10L, "demo", vector);

        server.verify();
    }

    @Test
    void searchSimilarShouldReturnResults() {
        List<Float> vector = vectorWithLength(QdrantConstants.VECTOR_SIZE);

        server.expect(once(), requestTo(BASE_URL + "/collections/" + COLLECTION))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo(BASE_URL + "/collections/" + COLLECTION + "/points/search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.vector", hasSize(QdrantConstants.VECTOR_SIZE)))
                .andExpect(jsonPath("$.limit", is(2)))
                .andExpect(jsonPath("$.filter.must[0].key", is("project")))
                .andExpect(jsonPath("$.filter.must[0].match.value", is("demo")))
                .andRespond(withSuccess("{\"result\":[{\"id\":1,\"score\":0.9,\"payload\":{\"chunkId\":1,\"documentId\":10,\"project\":\"demo\"}}]}", MediaType.APPLICATION_JSON));

        List<SearchResult> results = qdrantService.searchSimilar(vector, 2, " demo ");

        assertThat(results).hasSize(1);
        SearchResult result = results.get(0);
        assertThat(result.chunkId()).isEqualTo(1L);
        assertThat(result.documentId()).isEqualTo(10L);
        assertThat(result.project()).isEqualTo("demo");
        assertThat(result.score()).isEqualTo(0.9d);
    }

    @Test
    void storeEmbeddingShouldRejectWrongVectorLength() {
        List<Float> vector = vectorWithLength(2);

        assertThatThrownBy(() -> qdrantService.storeEmbedding(1L, 10L, "demo", vector))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(QdrantConstants.VECTOR_SIZE));
    }

    @Test
    void searchShouldReturnEmptyOnNotFound() {
        List<Float> vector = vectorWithLength(QdrantConstants.VECTOR_SIZE);

        server.expect(once(), requestTo(BASE_URL + "/collections/" + COLLECTION))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo(BASE_URL + "/collections/" + COLLECTION + "/points/search"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        List<SearchResult> results = qdrantService.searchSimilar(vector, 3, "demo");

        assertThat(results).isEmpty();
    }

    @Test
    void ensureCollectionShouldSurfaceCreationFailure() {
        List<Float> vector = vectorWithLength(QdrantConstants.VECTOR_SIZE);

        server.expect(once(), requestTo(BASE_URL + "/collections/" + COLLECTION))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        server.expect(once(), requestTo(BASE_URL + "/collections/" + COLLECTION))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("boom"));

        assertThatThrownBy(() -> qdrantService.storeEmbedding(1L, 10L, "demo", vector))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("create collection")
                .hasMessageContaining("500")
                .hasMessageContaining("boom");
    }

    @Test
    void searchShouldDropResultsMissingPayloadFields() {
        List<Float> vector = vectorWithLength(QdrantConstants.VECTOR_SIZE);

        server.expect(once(), requestTo(BASE_URL + "/collections/" + COLLECTION))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        String response = "{\"result\":[" +
                "{\"id\":1,\"score\":0.9,\"payload\":null}," +
                "{\"id\":2,\"score\":0.8,\"payload\":{\"chunkId\":2,\"project\":\"demo\"}}," +
                "{\"id\":3,\"score\":0.7,\"payload\":{\"chunkId\":3,\"documentId\":5}}" +
                "]}";

        server.expect(once(), requestTo(BASE_URL + "/collections/" + COLLECTION + "/points/search"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        List<SearchResult> results = qdrantService.searchSimilar(vector, 3, "demo");

        assertThat(results).isEmpty();
    }

    private List<Float> vectorWithLength(int size) {
        List<Float> vector = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            vector.add(0.1f);
        }
        return vector;
    }
}