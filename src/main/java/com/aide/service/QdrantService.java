package com.aide.service;

import com.aide.service.dto.qdrant.QdrantConstants;
import com.aide.service.dto.qdrant.collection.CreateCollectionRequest;
import com.aide.service.dto.qdrant.collection.VectorsConfig;
import com.aide.service.dto.qdrant.embedding.PointStruct;
import com.aide.service.dto.qdrant.embedding.QdrantPayload;
import com.aide.service.dto.qdrant.embedding.UpsertPointsRequest;
import com.aide.service.dto.qdrant.search.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class QdrantService {

    private static final Logger log = LoggerFactory.getLogger(QdrantService.class);

    private final RestTemplate restTemplate;
    private final String collectionName;
    private final String collectionUrl;
    private final String pointsUrl;
    private final AtomicBoolean collectionEnsured = new AtomicBoolean(false);
    private final double scoreThreshold;

    @Autowired
    public QdrantService(
            RestTemplate restTemplate,
            @Value("${qdrant.url:http://localhost:6333}") String baseUrl,
            @Value("${qdrant.collection:aide}") String collectionName,
            @Value("${app.search.score-threshold:0.65}") double scoreThreshold
    ) {
        this.restTemplate = restTemplate;
        String normalizedBaseUrl = trimTrailingSlash(requireNonBlank(baseUrl, "qdrant base url must not be null or blank"));
        this.collectionName = requireNonBlank(collectionName, "collectionName must not be null or blank");
        this.collectionUrl = normalizedBaseUrl + "/collections/" + this.collectionName;
        this.pointsUrl = this.collectionUrl + "/points";
        this.scoreThreshold = scoreThreshold;
    }

    public void storeEmbedding(Long chunkId, Long documentId, String project, List<Float> vector) {
        String normalizedProject = normalizeProject(project);
        validatePayload(chunkId, documentId, normalizedProject, vector);
        ensureCollectionExists();

        PointStruct point = new PointStruct(chunkId, vector, new QdrantPayload(chunkId, documentId, normalizedProject));
        UpsertPointsRequest request = new UpsertPointsRequest(List.of(point));

        try {
            log.debug("Upserting {} embedding(s) into collection {} with vector length {}", request.points().size(), collectionName, vector.size());
            restTemplate.put(pointsUrl, request);
        } catch (RestClientResponseException e) {
            log.error("Failed to upsert embedding into Qdrant collection {}", collectionName, e);
            throw new IllegalStateException(buildError("PUT", pointsUrl, e, "upsert embedding into Qdrant"), e);
        }
    }

    public List<SearchResult> searchSimilar(List<Float> vector, int topK, String project) {
        if (topK <= 0) {
            log.error("topK must be positive, but was {}", topK);
            throw new IllegalArgumentException("topK must be positive");
        }
        validateVector(vector);
        String normalizedProject = normalizeProject(project);
        ensureCollectionExists();

        QdrantCondition condition = new QdrantCondition("project", new QdrantMatch(normalizedProject));
        SearchRequest request = new SearchRequest(vector, topK, true, new QdrantFilter(List.of(condition)));

        long start = System.currentTimeMillis();
        try {
            SearchResponse response = restTemplate.postForObject(pointsUrl + "/search", request, SearchResponse.class);
            List<ScoredPoint> points = Optional.ofNullable(response)
                    .map(SearchResponse::result)
                    .orElse(Collections.emptyList());

            List<SearchResult> results = points.stream()
                    .filter(scoredPoint -> scoredPoint.score() > scoreThreshold)
                    .map(this::toSearchResult)
                    .flatMap(Optional::stream)
                    .toList();

            log.debug("Search in collection {} returned {} results in {} ms", collectionName, results.size(), System.currentTimeMillis() - start);
            return results;
        } catch (HttpClientErrorException.NotFound notFound) {
            log.debug("Collection {} not found during search; returning empty results", collectionName);
            return Collections.emptyList();
        } catch (RestClientResponseException e) {
            log.error("Failed to search in Qdrant collection {}", collectionName, e);
            throw new IllegalStateException(buildError("POST", pointsUrl + "/search", e, "search in Qdrant"), e);
        }
    }

    private Optional<SearchResult> toSearchResult(ScoredPoint point) {
        QdrantPayload payload = point.payload();
        if (payload == null) {
            log.debug("Skipping search result because payload is null (id={})", point.id());
            return Optional.empty();
        }

        Long chunkId = payload.chunkId();
        if (chunkId == null) {
            chunkId = toLong(point.id());
        }
        Long documentId = payload.documentId();
        String project = Optional.ofNullable(payload.project())
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElse(null);

        if (chunkId == null || documentId == null || project == null) {
            log.debug("Skipping search result because required payload is missing (id={}, payload={})", point.id(), payload);
            return Optional.empty();
        }

        return Optional.of(new SearchResult(chunkId, documentId, project, point.score()));
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        return null;
    }

    private void ensureCollectionExists() {
        if (collectionEnsured.get()) {
            return;
        }

        synchronized (collectionEnsured) {
            if (collectionEnsured.get()) {
                return;
            }

            verifyOrCreateCollection();
            collectionEnsured.set(true);
        }
    }

    private void verifyOrCreateCollection() {
        try {
            restTemplate.getForObject(collectionUrl, Object.class);
        } catch (HttpClientErrorException.NotFound notFound) {
            CreateCollectionRequest request = new CreateCollectionRequest(
                    new VectorsConfig(QdrantConstants.VECTOR_SIZE, QdrantConstants.DISTANCE_COSINE)
            );
            try {
                restTemplate.put(collectionUrl, request);
                log.debug("Created Qdrant collection {}", collectionName);
            } catch (RestClientResponseException createError) {
                log.error("Failed to create Qdrant collection {}", collectionName, createError);
                throw new IllegalStateException(buildError("PUT", collectionUrl, createError, "create collection " + collectionName), createError);
            }
        } catch (RestClientResponseException e) {
            log.error("Failed to check Qdrant collection {}", collectionName, e);
            throw new IllegalStateException(buildError("HEAD", collectionUrl, e, "check collection " + collectionName), e);
        }
    }

    private void validatePayload(Long chunkId, Long documentId, String project, List<Float> vector) {
        if (chunkId == null) {
            throw new IllegalArgumentException("chunkId must not be null");
        }
        if (documentId == null) {
            throw new IllegalArgumentException("documentId must not be null");
        }
        if (project == null || project.isBlank()) {
            throw new IllegalArgumentException("project must not be null or blank");
        }
        validateVector(vector);
    }

    private void validateVector(List<Float> vector) {
        if (vector == null || vector.isEmpty()) {
            throw new IllegalArgumentException("vector must not be null or empty");
        }
        if (vector.size() != QdrantConstants.VECTOR_SIZE) {
            throw new IllegalArgumentException("vector must have size " + QdrantConstants.VECTOR_SIZE);
        }
    }

    private String normalizeProject(String project) {
        if (project == null) {
            throw new IllegalArgumentException("project must not be null or blank");
        }
        String trimmed = project.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("project must not be null or blank");
        }
        return trimmed;
    }

    private String buildError(String method, String url, RestClientResponseException e, String action) {
        String body = Optional.ofNullable(e.getResponseBodyAsString())
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .map(this::truncateBody)
                .orElse("<empty>");
        return String.format("Failed to %s (status %d %s) at %s %s: %s", action, e.getStatusCode().value(), e.getStatusText(), method, url, body);
    }

    private String truncateBody(String body) {
        int max = 300;
        if (body.length() <= max) {
            return body;
        }
        return body.substring(0, max) + "...";
    }

    private String trimTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
