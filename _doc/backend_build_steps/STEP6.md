# Step 6 — QdrantService

Create a QdrantService.

Responsibilities:
- Create collection lazily if not exists (HEAD then PUT) using Qdrant REST API
- Store embedding vectors with payload:
    - chunkId
    - documentId
    - project
- Perform similarity search (topK) filtered by project
- Normalize and validate inputs (project non-blank, vector size matches 3072, topK > 0)

Constraints:
- Use RestTemplate against Qdrant REST API
- Cosine distance
- Vector size: 3072 (QdrantConstants)
- Package: com.aide.service
- On search, return empty list when collection is missing (404)
- Throw IllegalArgumentException for bad inputs; IllegalStateException for REST errors

Additionally:
- Unit tests with MockRestServiceServer covering collection creation, upsert, search, and error cases
- Keep a disabled integration test for a real Qdrant instance (e.g., Docker)
