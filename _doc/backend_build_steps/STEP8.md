# Step 8 — DocumentController

Create a DocumentController.

Endpoint:
POST /api/documents (consumes multipart/form-data)

Flow:
- Accept MultipartFile + project name as request params
- Delegate to DocumentService.uploadDocument
- Return 201 Created with UploadResponse {documentId, chunkCount}

Constraints:
- REST only
- No authentication yet
- Use @ControllerAdvice/GlobalExceptionHandler for consistent ApiError responses (400 on IllegalArgumentException, 413 on max size, 500 fallback)

Tests:
- WebMvcTest covering successful upload and bad-request path when the service rejects the file