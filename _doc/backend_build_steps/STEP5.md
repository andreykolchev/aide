# Step 5 — EmbeddingService

Create an EmbeddingService that calls Google GenAI REST API.

Constraints:
- Use Client from google-genai
- Read API key from Spring property `gemini.api-key` (overridable via environment) and model from `gemini.embeddings.model`
- Method: List<Float> embed(String text)
- Validate text is non-null/non-blank; throw IllegalArgumentException otherwise
- Call Gemini embeddings (`gemini-embedding-001`) and surface failures as IllegalStateException with useful message
- Require non-empty embedding response; typical size 3072 for current model
- Unit test with mocks; keep optional integration test disabled


- Package: com.aide.service
