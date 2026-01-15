'use client';

import { useState } from 'react';
import axios from 'axios';
import styles from './page.module.css';

interface SearchResult {
  documentId: number;
  documentName: string;
  documentPath: string;
  chunkId: number;
  content: string;
  score: number;
}

interface AskResponse {
  answer: string;
}

// Get API base URL from environment or use default
const getApiBaseUrl = () => {
  if (globalThis.window !== undefined) {
    // Check for environment variable (NEXT_PUBLIC_API_URL)
    const envUrl = process.env.NEXT_PUBLIC_API_URL;
    if (envUrl) return envUrl;
  }
  // Default to localhost:8080
  return 'http://localhost:8080';
};

export default function Home() {
  const [project, setProject] = useState('backend');
  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState('');
  const [sources, setSources] = useState<SearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [uploadLoading, setUploadLoading] = useState(false);
  const [uploadMessage, setUploadMessage] = useState('');
  const [apiBaseUrl] = useState(getApiBaseUrl());

  // Construct full API URL
  const getApiUrl = (endpoint: string) => {
    const base = apiBaseUrl.endsWith('/') ? apiBaseUrl.slice(0, -1) : apiBaseUrl;
    return `${base}${endpoint}`;
  };

  // Download document by ID (internal use only)
  const downloadDocument = async (documentId: number, documentName: string) => {
    try {
      const response = await axios.get(getApiUrl(`/api/documents/${documentId}`), {
        responseType: 'blob',
      });

      // Create a temporary URL for the blob and trigger download
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;

      // Extract filename from Content-Disposition header if available
      const contentDisposition = response.headers['content-disposition'];
      let fileName = `${documentName}`;
      if (contentDisposition) {
        const fileNameMatch = contentDisposition.match(/filename="?([^"]+)"?/);
        if (fileNameMatch) {
          fileName = fileNameMatch[1];
        }
      }

      link.setAttribute('download', fileName);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Error downloading document:', error);
    }
  };

  const handleAsk = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!question.trim()) return;

    setLoading(true);
    try {
      const response = await axios.post<AskResponse>(getApiUrl('/api/ask'), {
        question: question,
        project: project,
      });
      setAnswer(response.data.answer);
      setSources([]); // Clear full source details for ask
    } catch (error) {
      setAnswer('Error asking question. Please try again.');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!question.trim()) return;

    setLoading(true);
    try {
      const response = await axios.post<SearchResult[]>(getApiUrl('/api/search'), {
        query: question,
        project: project,
      });
      setSources(response.data || []);
      setAnswer('');
    } catch (error) {
      setAnswer('Error searching. Please try again.');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) {
      setUploadMessage('Please select a file');
      return;
    }

    setUploadLoading(true);
    setUploadMessage('');
    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('project', project);

      await axios.post(getApiUrl('/api/documents'), formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      setUploadMessage('File uploaded successfully!');
      setFile(null);
      // Reset file input
      const input = document.getElementById('file-input') as HTMLInputElement;
      if (input) input.value = '';
    } catch (error) {
      setUploadMessage('Error uploading file. Please try again.');
      console.error(error);
    } finally {
      setUploadLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <h1>🤖 AIDE</h1>
        <p>AI-powered documentation search engine</p>
      </header>

      <main className={styles.main}>
        <div className={styles.sidebar}>
          <section className={styles.section}>
            <h2>Project</h2>
            <input
              type="text"
              value={project}
              onChange={(e) => setProject(e.target.value)}
              placeholder="Enter project name"
              className={styles.input}
            />
          </section>

          <section className={styles.section}>
            <h2>Upload Documentation</h2>
            <form onSubmit={handleUpload} className={styles.form}>
              <input
                id="file-input"
                type="file"
                onChange={(e) => setFile((e.target as HTMLInputElement).files?.[0] || null)}
                className={styles.fileInput}
                accept=".pdf,.txt,.md,.doc,.docx"
              />
              <button
                type="submit"
                disabled={uploadLoading || !file}
                className={styles.button}
              >
                {uploadLoading ? 'Uploading...' : 'Upload File'}
              </button>
              {uploadMessage && (
                <p
                  className={
                    uploadMessage.includes('successfully')
                      ? styles.successMessage
                      : styles.errorMessage
                  }
                >
                  {uploadMessage}
                </p>
              )}
            </form>
          </section>
        </div>

        <div className={styles.content}>
          <section className={styles.section}>
            <h2>Ask a Question</h2>
            <form onSubmit={handleAsk} className={styles.form}>
              <textarea
                value={question}
                onChange={(e) => setQuestion(e.target.value)}
                placeholder="Ask a question about your documentation..."
                className={styles.textarea}
                rows={4}
              />
              <div className={styles.buttonGroup}>
                <button
                  type="submit"
                  disabled={loading}
                  className={styles.button}
                >
                  {loading ? 'Asking...' : 'Ask'}
                </button>
                <button
                  type="button"
                  onClick={handleSearch}
                  disabled={loading}
                  className={styles.secondaryButton}
                >
                  {loading ? 'Searching...' : 'Search Only'}
                </button>
              </div>
            </form>
          </section>

          {answer && (
            <section className={styles.section}>
              <h2>Answer</h2>
              <div className={styles.answer}>{answer}</div>
            </section>
          )}

          {sources.length > 0 && (
            <section className={styles.section}>
              <h2>Sources ({sources.length})</h2>
              <div className={styles.sources}>
                {sources.map((source) => (
                  <button
                    key={`${source.documentId}-${source.chunkId}`}
                    className={styles.sourceItem}
                    onClick={() => downloadDocument(source.documentId, source.documentName)}
                  >
                    <div className={styles.sourceMeta}>
                      <span className={styles.sourceId}>
                        {source.documentName}, Chunk {source.chunkId}
                      </span>
                      <span className={styles.sourceScore}>
                        Score: {(source.score * 100).toFixed(1)}%
                      </span>
                    </div>
                    <p className={styles.sourceContent}>{source.content}</p>
                  </button>
                ))}
              </div>
            </section>
          )}
        </div>
      </main>
    </div>
  );
}
