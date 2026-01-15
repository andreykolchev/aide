# Step 1 — Model

Create JPA entities for an MVP AI documentation system.

Constraints:
- Java 25
- Spring Boot 4
- Use jakarta.persistence
- With Lombok
- Keep it minimal
- Package: com.aide.model

Entities:
1. Document
    - id (Long, PK)
    - name (String)
    - project (String)
    - filePath (String)
    - uploadedAt (Instant)

2. DocumentChunk
    - id (Long, PK)
    - documentId (Long)
    - chunkIndex (Integer)
    - content (String, up to 5000 chars)