# Step 9 — SearchController

Create a SearchController.

Endpoint:
POST /api/search

Input:
{ "query": "...", "project": "..." }

Flow:
- Embed query
- Search Qdrant (top 5)
- Fetch chunk content from DB
- Return ranked results with similarity score