# Step 7 — DocumentService

Create a DocumentService that orchestrates ingestion, chunking, storage, and vector indexing for uploaded documents.

Responsibilities:
- Accept MultipartFile + project name
- Validate/normalize project (reject null or blank; trim whitespace)
- Ingest file to extract text and persist the source file path
- Save Document entity with name, project, filePath, uploadedAt
- Chunk extracted text
- If no chunks, return UploadResponse with chunkCount=0 and skip chunk persistence/embeddings
- Persist DocumentChunk records (documentId, chunkIndex, content)
- Generate embeddings per chunk and store them in Qdrant with payload: chunkId, documentId, project
- Return UploadResponse containing documentId and chunkCount

Constraints:
- Package: com.aide.service
- Use existing services/repositories: IngestionService, ChunkingService, EmbeddingService, QdrantService, DocumentRepository, DocumentChunkRepository
- Do not embed or push to Qdrant when no chunks are produced
- Throw clear error when project is missing or blank

Tests:
- Add unit tests covering the happy path and the empty-chunk path