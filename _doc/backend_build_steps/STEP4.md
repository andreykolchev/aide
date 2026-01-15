# Step 4 — ChunkingService

Create a ChunkingService.

Behavior:
- Input: large String
- Output: List<String> chunks
- Chunk size: ~500 tokens equivalent (use characters)
- Overlap: 50 characters
- Preserve order
- Return empty list when input is null or blank

- Package: com.aide.service
