# Step 10 — AskController

Create an AskController.

Endpoint:
POST /api/ask

Input:
{ "question": "...", "project": "..." }

Flow:
- Embed question
- Retrieve top 5 chunks
- Build prompt using retrieved content only
- Call Gemini chat completion
- Return answer + source chunk IDs

Constraints:
- No hallucinations
- Explicit prompt grounding