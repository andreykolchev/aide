# Step 3 — IngestionService

Create an IngestionService.

Responsibilities:
- Accept a MultipartFile, validate it is non-null/non-empty and has an original filename.
- Persist the file to the local filesystem under `app.ingestion.docs-path` (default: `./data/docs`), preserving the original filename and ensuring the directory exists.
- Extract text:
    - PDF: Apache PDFBox Loader + PDFTextStripper
    - Plain text: read file contents directly
    - Reject unsupported types (by extension or content type)
- Return an `IngestionResult` containing the extracted text and storedPath

Constraints:
- Package: com.aide.service
- Throw IllegalArgumentException for invalid input/unsupported type
- Wrap I/O failures as IllegalStateException
- No controller yet