import express from 'express';
import { createProxyMiddleware } from 'http-proxy-middleware';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const app = express();

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';
const PORT = process.env.PORT || 3000;

// Forward /api requests to the Spring Boot backend (same as Vite's dev proxy)
app.use(
  createProxyMiddleware({
    target: BACKEND_URL,
    changeOrigin: true,
    pathFilter: '/api',
  })
);

// Serve the built React app
app.use(express.static(path.join(__dirname, 'dist')));

// Send index.html for all other routes so React Router pages (/staff etc.) work on refresh
app.use((req, res) => {
  res.sendFile(path.join(__dirname, 'dist', 'index.html'));
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Frontend running on port ${PORT}, proxying /api to ${BACKEND_URL}`);
});