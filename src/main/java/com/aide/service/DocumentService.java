package com.aide.service;

import com.aide.controller.dto.UploadResponse;
import com.aide.model.Document;
import com.aide.model.DocumentChunk;
import com.aide.repository.DocumentChunkRepository;
import com.aide.repository.DocumentRepository;
import com.aide.service.dto.IngestionResult;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DocumentService {

    private final IngestionService ingestionService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;


    public DocumentService(
            IngestionService ingestionService,
            ChunkingService chunkingService,
            EmbeddingService embeddingService,
            QdrantService qdrantService,
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository
    ) {
        this.ingestionService = ingestionService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.qdrantService = qdrantService;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
    }

    @Transactional
    public UploadResponse uploadDocument(MultipartFile file, String project) {
        //project validation
        String normalizedProject = normalizeProject(project);

        //document ingestion
        IngestionResult ingestionResult = ingestionService.ingest(file);

        //save document to DB
        Document document = new Document();
        document.setName(file.getOriginalFilename());
        document.setProject(normalizedProject);
        document.setFilePath(ingestionResult.storedPath().toString());
        document.setUploadedAt(Instant.now());
        document = documentRepository.save(document);

        //save chunks to DB
        List<String> chunks = chunkingService.chunk(ingestionResult.text());
        if (chunks.isEmpty()) {
            return new UploadResponse(document.getId(), 0);
        }
        List<DocumentChunk> chunkEntities = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(document.getId());
            chunk.setChunkIndex(i);
            chunk.setContent(chunks.get(i));
            chunkEntities.add(chunk);
        }
        List<DocumentChunk> savedChunks = documentChunkRepository.saveAll(chunkEntities);

        for (DocumentChunk chunk : savedChunks) {
            //get embedding from AI service
            List<Float> embedding = embeddingService.embed(chunk.getContent());
            //store embedding vector in Qdrant
            qdrantService.storeEmbedding(chunk.getId(), document.getId(), normalizedProject, embedding);
        }

        return new UploadResponse(document.getId(), savedChunks.size());
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

    public ResponseEntity<Resource> downloadDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with id: " + id));

        Path filePath = getSecurePath(document.getFilePath());
        FileSystemResource resource = new FileSystemResource(filePath);

        if (!resource.exists()) {
            throw new IllegalStateException("File not found on disk: " + filePath);
        }

        String fileName = document.getName();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    private Path getSecurePath(String filePath) {
        Path basePath = Paths.get("./data/docs").toAbsolutePath().normalize();
        Path fullPath = basePath.resolve(filePath).normalize();

        if (!fullPath.startsWith(basePath)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        return fullPath;
    }
}