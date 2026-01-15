# 🤖 AIDE

AI-powered documentation search engine.

This service ingests project documentation (TXT/PDF), chunks it, embeds the chunks using Gemini embeddings, stores vectors in Qdrant, and provides search + Q&A endpoints.

## 🛠 Tech Stack

- ☕ Java 25
- 🍃 Spring Boot 4.0.1 (WebMVC)
- 🗄 PostgreSQL (metadata + stored chunks)
- 🔍 Qdrant (vector store)
- ✨ Google Gemini (embeddings + chat)

## 📋 Prerequisites

### Backend
- ☕ Java 25
- 📦 Maven
- 🐳 Docker + Docker Compose
- 🔑 A Gemini API key

### Frontend (optional)
- 🟢 Node.js 18+ and npm

## ⚙️ Configuration

Default configuration lives in `src/main/resources/application.yaml`.

- **HTTP port**: `8080`
- **Postgres**:
  - URL: `jdbc:postgresql://localhost:5432/aide`
  - User: `analyst`
  - Password: `analyst`
- **Qdrant**:
  - URL: `http://localhost:6333`
  - Collection: `aide` (auto-created on first use)
- **Gemini**:
  - `gemini.api-key` is required for embeddings/chat

### 🔐 Setting the Gemini API key

The code reads `gemini.api-key` (Spring property: `gemini.api-key`). The easiest way is to export an environment variable before starting the app:

```bash
export GEMINI_API_KEY="your_api_key_here"
```

Spring Boot’s relaxed binding maps `GEMINI_API_KEY` -> `gemini.api-key`.

## 🚀 Run Locally

### 1) Start infrastructure (Postgres + Qdrant)

From the repo root:

```bash
docker compose up -d
```

This starts:

- Postgres on `localhost:5432`
- Qdrant on `localhost:6333` (REST) and `localhost:6334` (gRPC)

### 2) Start the Spring Boot app

```bash
export GEMINI_API_KEY="your_api_key_here"
mvn spring-boot:run
```

The app will be available at:

- `http://localhost:8080`

Swagger UI (OpenAPI):

- `http://localhost:8080/swagger-ui/index.html`

### 3) Start the Next.js frontend (optional)

The frontend is a Next.js web application that provides a user-friendly interface for interacting with AIDE. It allows you to upload documents, search the knowledge base, and ask questions with AI-powered responses backed by your documentation.

From the `frontend` directory:

```bash
npm install
npm run dev
```

The frontend will be available at:

- `http://localhost:3000`

The frontend is configured to connect to the backend API at `http://localhost:8080` by default. You can override this by setting the `NEXT_PUBLIC_API_URL` environment variable.

## 🔌 API

### 📤 Upload a document

`POST /api/documents` (multipart)

- `file`: TXT or PDF
- `project`: project name (used for filtering)

Example:

```bash
curl -X POST "http://localhost:8080/api/documents" \
  -F "file=@./data/docs/architecture-overview.txt" \
  -F "project=aide"
```

Response:

- `documentId`: stored document id
- `chunkCount`: number of created chunks

### 🔍 Search

`POST /api/search`

Body:

```json
{
  "query": "how does ingestion work?",
  "project": "aide"
}
```

Example:

```bash
curl -X POST "http://localhost:8080/api/search" \
  -H "Content-Type: application/json" \
  -d '{"query":"how does ingestion work?","project":"aide"}'
```

Response is a list of items:

- `documentId`
- `chunkId`
- `content`
- `score`

### ❓ Ask (Q&A)

`POST /api/ask`

Body:

```json
{
  "question": "What is the chunk size used for ingestion?",
  "project": "aide"
}
```

Example:

```bash
curl -X POST "http://localhost:8080/api/ask" \
  -H "Content-Type: application/json" \
  -d '{"question":"What is the chunk size used for ingestion?","project":"aide"}'
```

Response:

- `answer`: Gemini answer grounded in retrieved chunks
- `sources`: list of chunk IDs used as context

## 📝 Notes

- Uploaded files are persisted to `./data/docs` by default (configurable via `app.ingestion.docs-path`).
- Supported formats:
  - `.pdf` (extracted with PDFBox)
  - `.txt` (plain text)

## 🧪 Run Tests

```bash
mvn test
```

## 🔧 Troubleshooting

- If you get database connection errors, ensure Postgres is running and port `5432` is not taken.
- If you get Qdrant errors, ensure Qdrant is running on `http://localhost:6333`.
- If embeddings/chat calls fail, ensure `GEMINI_API_KEY` is set and valid.
