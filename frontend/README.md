# AIDE Frontend

A simple Next.js React frontend for the AIDE (AI-powered Documentation Search Engine).

## Features

- 🤖 Ask questions about your documentation
- 🔍 Search for relevant content
- 📤 Upload documentation files
- 📊 View search results with relevance scores
- 📱 Responsive design

## Tech Stack

- **Next.js 15** - React framework with static export
- **React 19** - UI library
- **TypeScript** - Type safety
- **Axios** - HTTP client
- **CSS Modules** - Scoped styling

## Quick Start

### Prerequisites

- Node.js 18+
- npm or yarn

### Setup

```bash
cd frontend
npm install
```

### Development Server

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

### Build for Production

```bash
npm run build
```

This generates static files that are embedded into the Spring Boot JAR at:
```
src/main/resources/static/
```

## API Integration

The frontend communicates with the Spring Boot backend at `http://localhost:8080`:

### POST /api/ask
Ask a question about documentation
```json
{
  "question": "How do I use this feature?",
  "project": "my-project"
}
```

### POST /api/search
Search for content
```json
{
  "query": "search term",
  "project": "my-project"
}
```

### POST /api/documents
Upload documentation (multipart form data)
- `file`: The documentation file
- `project`: Project name

## Project Structure

```
frontend/
├── app/
│   ├── layout.tsx      - Root layout with metadata
│   ├── page.tsx        - Main page component (Ask, Search, Upload)
│   ├── globals.css     - Global styles
│   └── page.module.css - Component styles
├── next.config.js      - Next.js configuration
├── tsconfig.json       - TypeScript configuration
└── package.json        - Dependencies and scripts
```

## Main Component (page.tsx)

The main component handles:
- **Ask Section**: Send questions to the RAG pipeline
- **Search Section**: Search for relevant documentation chunks
- **Upload Section**: Upload new documentation files
- **Project Selection**: Switch between different projects

All API communication uses Axios with configurable base URL from `NEXT_PUBLIC_API_URL` environment variable.

## Styling

- Global styles in `globals.css` (reset and basic styles)
- Component styles in `page.module.css` (520+ lines of CSS Modules)
- Responsive design with breakpoints for mobile, tablet, and desktop
- CSS Grid and Flexbox layouts

## Deployment

The frontend is built as a static Next.js export and embedded into the Spring Boot JAR, creating a single deployable artifact:

1. Built with `npm run build`
2. Output to `src/main/resources/static/`
3. Served by Spring Boot at `/`

## Environment Variables

- `NEXT_PUBLIC_API_URL` - Backend API base URL (defaults to `http://localhost:8080`)

## Development Notes

- Uses 'use client' directive for client-side rendering
- State management with React hooks (useState)
- Responsive CSS Grid layout
- Loading states and error handling for all API calls
- File input validation and upload feedback
